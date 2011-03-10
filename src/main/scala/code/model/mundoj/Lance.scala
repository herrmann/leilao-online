package code.model.mundoj

import java.math.MathContext
import net.liftweb.mapper._
import code.model.User

object Lance extends Lance with LongKeyedMetaMapper[Lance] {
  override def fieldOrder = List(id)
}

class Lance extends LongKeyedMapper[Lance] with IdPK {
  import MathContext._
  def getSingleton = Lance
  object usuario extends MappedLongForeignKey(this, User)
  object leilao extends MappedLongForeignKey(this, Leilao)
  object item extends MappedLongForeignKey(this, Item)
  object valor extends MappedDecimal(this, DECIMAL64, 2)
}