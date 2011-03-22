package code.comet

import net.liftweb.actor.LiftActor
import scala.collection.mutable.Map
import code.model.mundoj.{Item, Lance, Usuario}
import net.liftweb.common.{Empty, Box}
import xml.NodeSeq
import net.liftweb.http._
import net.liftweb.http.js._
import net.liftweb.http.js.JE._
import net.liftweb.http.js.JsCmds._

object Mensagens {
  case class RegistrarCliente(cliente: CometActor, itemId: Long)
  case class DesregistrarCliente(cliente: CometActor, itemId: Long)
  case class DarLance(itemId: Long, valor: Double, usuario: Usuario)
  case class PegarMaiorLance(itemId: Long)
  case class MaiorLance(lance: Box[Lance])
}

object GerenciadorDoLeilao extends LiftActor {
  import Mensagens._
  private val clientes = Map[Long, List[CometActor]]()

  override protected def messageHandler = {
    case RegistrarCliente(cliente, itemId)    => registraClienteParaItem(cliente, itemId)
    case DesregistrarCliente(cliente, itemId) => desregistraClienteParaItem(cliente, itemId)
    case DarLance(itemId, valor, usuario)     => darLance(itemId, valor, usuario)
    case PegarMaiorLance(itemId)              => reply(pegarMaiorLance(itemId))
  }

  def registraClienteParaItem(cliente: CometActor, itemId: Long) = {
    clientes.get(itemId) match {
      case Some(lista) => clientes += (itemId -> lista.+:(cliente))
      case None        => clientes += (itemId -> List(cliente))
    }
  }

  def desregistraClienteParaItem(cliente: CometActor, itemId: Long) = {
    clientes.get(itemId) match {
      case Some(lista) => clientes += (itemId -> lista.filterNot(e => e == cliente))
      case None        => /* nao ha item, logo nao ha cliente */
    }
  }

  def darLance(itemId: Long, valor: Double, usuario: Usuario) = {
    val lance = Lance.create
    lance.item(Item.findByKey(itemId))
    lance.valor(valor)
    lance.usuario(usuario)
    atualizaClientes(itemId, lanceMaisAlto(itemId))
  }

  def pegarMaiorLance(itemId: Long): MaiorLance = {
    MaiorLance(lanceMaisAlto(itemId))
  }

  def lanceMaisAlto(itemId: Long): Box[Lance] = {
    Lance.lanceMaisAlto(itemId)
  }

  def atualizaClientes(itemId: Long, lanceMaisAlto: Box[Lance]) = {
    clientes.get(itemId).foreach{
      atores => atores.map(atorCliente => atorCliente ! MaiorLance(lanceMaisAlto))
    }
  }
}


class ClienteDoLeilao extends CometActor {
  import Mensagens._

  object itemId extends RequestVar[String](S.param("id").openOr(""))
  var maiorLance: Box[Lance] = Empty


  def lance (in: NodeSeq): NodeSeq = {
    bind("entry", in, "itemId"-> SHtml.text(itemId.is, itemId(_), "type" -> "hidden"),
                      "submit"-> SHtml.ajaxButton("Dar Lance", JsRaw("$('#valorLance').attr('value')"), novoLance _))
  }

  def novoLance(valor: String): JsCmd = {
    val valorEntrado = valor.toDouble
    GerenciadorDoLeilao ! DarLance(itemId, valorEntrado, Usuario.currentUser.open_!)
    Noop
  }

  override protected def localShutdown = {
    GerenciadorDoLeilao ! DesregistrarCliente(this, itemId)
  }

  override protected def localSetup() = {
    GerenciadorDoLeilao ! RegistrarCliente(this, itemId)
  }

  override def lowPriority: PartialFunction[Any, Unit] = {
    case MaiorLance(lance) => atualizaMaiorLance(lance)
  }

  def atualizaMaiorLance(lance: Box[Lance]) = {
    maiorLance = lance
    reRender(false)
  }



  def render = {
    def valorDoMaiorLance = {
      maiorLance.map(_.valor.is.toString).openOr("Não há lances")
    }
    bind("lance_mais_alto"  -> valorDoMaiorLance)
  }

  implicit def requestVarToLong(v: RequestVar[String]): Long = v.is.toLong
}


