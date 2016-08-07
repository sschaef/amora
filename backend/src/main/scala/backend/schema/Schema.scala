package backend.schema

trait Schema
final case class Project(name: String) extends Schema
final case class Artifact(owner: Project, organization: String, name: String, version: String) extends Schema
final case class File(owner: Schema, name: String) extends Schema
final case class Package(name: String, owner: Schema) extends Schema
final case class Class(name: String, owner: Schema) extends Schema

object Schema {

  def mkSparqlUpdate(schemas: Seq[Schema]): String = {
    val sb = new StringBuilder

    def mkShortId(s: Schema): String = s match {
      case Project(name) ⇒
        name
      case Artifact(owner, organization, name, version) ⇒
        s"${mkShortId(owner)}/$organization/$name/$version"
      case File(owner, name) ⇒
        s"${mkShortId(owner)}/$name"
      case Package(name, owner) ⇒
        s"${mkShortId(owner)}/$name"
      case Class(name, owner) ⇒
        s"${mkShortId(owner)}/$name"
    }

    def mkId(s: Schema) = s match {
      case _: Project ⇒
        s"http://amora.center/kb/amora/Project/0.1/${mkShortId(s)}"
      case _: Artifact ⇒
        s"http://amora.center/kb/amora/Artifact/0.1/${mkShortId(s)}"
      case _: File ⇒
        s"http://amora.center/kb/amora/File/0.1/${mkShortId(s)}"
      case _: Package ⇒
        s"http://amora.center/kb/amora/Package/0.1/${mkShortId(s)}"
      case _: Class ⇒
        s"http://amora.center/kb/amora/Class/0.1/${mkShortId(s)}"
    }

    def mkDefn(s: Schema) = s match {
      case _: Project ⇒
        s"http://amora.center/kb/amora/Schema/0.1/Project/0.1"
      case _: Artifact ⇒
        s"http://amora.center/kb/amora/Schema/0.1/Artifact/0.1"
      case _: File ⇒
        s"http://amora.center/kb/amora/Schema/0.1/File/0.1"
      case _: Package ⇒
        s"http://amora.center/kb/amora/Schema/0.1/Package/0.1"
      case _: Class ⇒
        s"http://amora.center/kb/amora/Schema/0.1/Class/0.1"
    }

    def mk(s: Schema): String = s match {
      case Project(name) ⇒
        val id = mkId(s)
        val defn = mkDefn(s)
        val tpe = "http://schema.org/Text"
        sb.append(s"""|  <$id> a <$defn/> .
                      |  <$id> <$defn/name> "$name"^^<$tpe> .
        |""".stripMargin)
        id
      case Artifact(owner, organization, name, version) ⇒
        val oid = mk(owner)
        val id = mkId(s)
        val defn = mkDefn(s)
        val tpe = "http://schema.org/Text"
        sb.append(s"""|  <$id> a <$defn/> .
                      |  <$id> <$defn/owner> <$oid> .
                      |  <$id> <$defn/organization> "$organization"^^<$tpe> .
                      |  <$id> <$defn/name> "$name"^^<$tpe> .
                      |  <$id> <$defn/version> "$version"^^<$tpe> .
        |""".stripMargin)
        id
      case File(owner, fname) ⇒
        val oid = mk(owner)
        val id = mkId(s)
        val defn = mkDefn(s)
        val tpe = "http://schema.org/Text"
        sb.append(s"""|  <$id> a <$defn/> .
                      |  <$id> <$defn/owner> <$oid> .
                      |  <$id> <$defn/name> "$fname"^^<$tpe> .
        |""".stripMargin)
        id
      case Package(name, parent) ⇒
        val oid = mk(parent)
        val id = mkId(s)
        val defn = mkDefn(s)
        sb.append(s"""|  <$id> a <$defn/> .
                      |  <$id> <$defn/owner> <$oid> .
                      |  <$id> <$defn/name> "$name"^^<http://schema.org/Text> .
        |""".stripMargin)
        id
      case Class(name, parent) ⇒
        val oid = mk(parent)
        val id = mkId(s)
        val defn = mkDefn(s)
        sb.append(s"""|  <$id> a <$defn/> .
                      |  <$id> <$defn/owner> <$oid> .
                      |  <$id> <$defn/name> "$name"^^<http://schema.org/Text> .
        |""".stripMargin)
        id
    }

    sb.append("INSERT DATA {\n")
    schemas foreach mk
    sb.append("}")
    sb.toString()
  }

}