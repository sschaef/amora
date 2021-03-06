package amora.backend.indexer

import java.net.URLEncoder

import org.junit.Test

import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.Accept
import amora.backend.CustomContentTypes
import amora.backend.schema._
import amora.converter.protocol._

class IndexerTest extends RestApiTest {
  import amora.TestUtils._

  @Test
  def sparql_get_requests_are_possible(): Unit = {
    val query = "query="+URLEncoder.encode("select * where {?s ?p ?o} limit 3", "UTF-8")
    testReq(get(s"http://amora.center/sparql?$query")) {
      status === StatusCodes.OK
    }
  }

  @Test
  def sparql_get_request_misses_query_param(): Unit = {
    val query = URLEncoder.encode("select * where {?s ?p ?o} limit 3", "UTF-8")
    testReq(get(s"http://amora.center/sparql?$query")) {
      status === StatusCodes.BadRequest
    }
  }

  @Test
  def sparql_post_requests_are_possible(): Unit = {
    testReq(post("http://amora.center/sparql", "select * where {?s ?p ?o} limit 3", header = Accept(CustomContentTypes.`application/sparql-results+json`))) {
      status === StatusCodes.OK
    }
  }

  @Test
  def missing_accept_header_for_sparql_post_requests(): Unit = {
    testReq(post("http://amora.center/sparql", "select * where {?s ?p ?o} limit 3")) {
      status === StatusCodes.NotAcceptable
    }
  }

  @Test
  def encoded_sparql_post_requests_are_possible(): Unit = {
    val query = "query="+URLEncoder.encode("select * where {?s ?p ?o} limit 3", "UTF-8")
    val e = HttpEntity(CustomContentTypes.`application/x-www-form-urlencoded(UTF-8)`, query)
    testReq(post("http://amora.center/sparql", e, header = Accept(CustomContentTypes.`application/sparql-results+json`))) {
      status === StatusCodes.OK
    }
  }

  @Test
  def encoded_sparql_post_request_misses_query_param(): Unit = {
    val query = URLEncoder.encode("select * where {?s ?p ?o} limit 3", "UTF-8")
    val e = HttpEntity(CustomContentTypes.`application/x-www-form-urlencoded(UTF-8)`, query)
    testReq(post("http://amora.center/sparql", e, header = Accept(CustomContentTypes.`application/sparql-results+json`))) {
      status === StatusCodes.BadRequest
    }
  }

  @Test
  def invalid_content_type_for_sparql_post_requests(): Unit = {
    val e = HttpEntity(CustomContentTypes.`text/turtle(UTF-8)`, "invalid query")
    testReq(post("http://amora.center/sparql", e, header = Accept(CustomContentTypes.`application/sparql-results+json`))) {
      status === StatusCodes.UnsupportedMediaType
    }
  }

  @Test
  def syntax_error_in_sparql_post_request(): Unit = {
    testReq(post("http://amora.center/sparql", "syntax error", header = Accept(CustomContentTypes.`application/sparql-results+json`))) {
      status === StatusCodes.InternalServerError
    }
  }

  @Test
  def syntax_error_in_turtle_update(): Unit = {
    val e = HttpEntity(CustomContentTypes.`text/turtle(UTF-8)`, s"syntax error")
    testReq(post("http://amora.center/turtle-update", e)) {
      status === StatusCodes.InternalServerError
    }
  }

  @Test
  def invalid_content_type_for_turtle_update_post_requests(): Unit = {
    val e = HttpEntity(CustomContentTypes.`application/sparql-query(UTF-8)`, "invalid query")
    testReq(post("http://amora.center/turtle-update", e)) {
      status === StatusCodes.UnsupportedMediaType
    }
  }

  @Test
  def add_single_project_through_turtle_update_post_request(): Unit = {
    val q = Schema.mkTurtleString(Seq(Project("p")))
    val e = HttpEntity(CustomContentTypes.`text/turtle(UTF-8)`, q)
    testReq(post("http://amora.center/turtle-update", e)) {
      status === StatusCodes.OK
    }
    testReq((post("http://amora.center/sparql", """
      prefix p:<http://amora.center/kb/amora/Schema/Project/>
      select * where {
        [a p:] p:name ?name .
      }
    """, header = Accept(CustomContentTypes.`application/sparql-results+json`)))) {
      status === StatusCodes.OK
      resultSetAsData(respAsResultSet) === Seq(Seq(Data("name", "p")))
    }
  }

  @Test
  def syntax_error_in_sparql_update(): Unit = {
    testReq(post("http://amora.center/sparql-update", s"syntax error")) {
      status === StatusCodes.InternalServerError
    }
  }

  @Test
  def invalid_content_type_for_sparql_update_post_requests(): Unit = {
    val e = HttpEntity(CustomContentTypes.`text/turtle(UTF-8)`, "invalid query")
    testReq(post("http://amora.center/sparql-update", e)) {
      status === StatusCodes.UnsupportedMediaType
    }
  }

  @Test
  def add_single_project(): Unit = {
    val q = Schema.mkSparqlUpdate(Seq(Project("p")))
    testReq(post("http://amora.center/sparql-update", q)) {
      status === StatusCodes.OK
    }
    testReq((post("http://amora.center/sparql", """
      prefix p:<http://amora.center/kb/amora/Schema/Project/>
      select * where {
        [a p:] p:name ?name .
      }
    """, header = Accept(CustomContentTypes.`application/sparql-results+json`)))) {
      status === StatusCodes.OK
      resultSetAsData(respAsResultSet) === Seq(Seq(Data("name", "p")))
    }
  }

  @Test
  def encoded_sparql_update_post_requests_are_possible(): Unit = {
    val q = Schema.mkSparqlUpdate(Seq(Project("p")))
    val query = "query="+URLEncoder.encode(q, "UTF-8")
    val e = HttpEntity(CustomContentTypes.`application/x-www-form-urlencoded(UTF-8)`, query)
    testReq(post("http://amora.center/sparql-update", e)) {
      status === StatusCodes.OK
    }
    testReq((post("http://amora.center/sparql", """
      prefix p:<http://amora.center/kb/amora/Schema/Project/>
      select * where {
        [a p:] p:name ?name .
      }
    """, header = Accept(CustomContentTypes.`application/sparql-results+json`)))) {
      status === StatusCodes.OK
      resultSetAsData(respAsResultSet) === Seq(Seq(Data("name", "p")))
    }
  }

  @Test
  def encoded_sparql_update_post_request_misses_query_param(): Unit = {
    val q = Schema.mkSparqlUpdate(Seq(Project("p")))
    val query = URLEncoder.encode(q, "UTF-8")
    val e = HttpEntity(CustomContentTypes.`application/x-www-form-urlencoded(UTF-8)`, query)
    testReq(post("http://amora.center/sparql-update", e)) {
      status === StatusCodes.BadRequest
    }
  }

  @Test
  def add_multiple_projects(): Unit = {
    val q = Schema.mkSparqlUpdate(Seq(Project("p1"), Project("p2")))
    testReq(post("http://amora.center/sparql-update", q)) {
      status === StatusCodes.OK
    }
    testReq((post("http://amora.center/sparql", """
      prefix p:<http://amora.center/kb/amora/Schema/Project/>
      select ?name where {
        [a p:] p:name ?name .
      }
      order by ?name
    """, header = Accept(CustomContentTypes.`application/sparql-results+json`)))) {
      status === StatusCodes.OK
      resultSetAsData(respAsResultSet) === Seq(
          Seq(Data("name", "p1")),
          Seq(Data("name", "p2")))
    }
  }

  @Test
  def add_single_artifact(): Unit = {
    val a = Artifact(Project("p"), "o", "n", "v1")
    val q = Schema.mkSparqlUpdate(Seq(a))
    testReq(post("http://amora.center/sparql-update", q)) {
      status === StatusCodes.OK
    }
    testReq((post("http://amora.center/sparql", """
      prefix a:<http://amora.center/kb/amora/Schema/Artifact/>
      select ?organization ?name ?version where {
        [a a:] a:organization ?organization ; a:name ?name ; a:version ?version .
      }
    """, header = Accept(CustomContentTypes.`application/sparql-results+json`)))) {
      status === StatusCodes.OK
      resultSetAsData(respAsResultSet) === Seq(
          Seq(Data("organization", "o"), Data("name", "n"), Data("version", "v1")))
    }
  }

  @Test
  def add_multiple_artifacts(): Unit = {
    val p = Project("p")
    val a1 = Artifact(p, "o1", "n1", "v1")
    val a2 = Artifact(p, "o2", "n2", "v2")
    val q = Schema.mkSparqlUpdate(Seq(a1, a2))
    testReq(post("http://amora.center/sparql-update", q)) {
      status === StatusCodes.OK
    }
    testReq((post("http://amora.center/sparql", """
      prefix a:<http://amora.center/kb/amora/Schema/Artifact/>
      select ?organization ?name ?version where {
        [a a:] a:organization ?organization ; a:name ?name ; a:version ?version .
      }
      order by ?organization ?name ?version
    """, header = Accept(CustomContentTypes.`application/sparql-results+json`)))) {
      status === StatusCodes.OK
      resultSetAsData(respAsResultSet) === Seq(
          Seq(Data("organization", "o1"), Data("name", "n1"), Data("version", "v1")),
          Seq(Data("organization", "o2"), Data("name", "n2"), Data("version", "v2")))
    }
  }

  @Test
  def multiple_artifacts_can_belong_to_the_same_project(): Unit = {
    val p = Project("p")
    val a1 = Artifact(p, "o1", "n1", "v1")
    val a2 = Artifact(p, "o2", "n2", "v2")
    val q = Schema.mkSparqlUpdate(Seq(a1, a2))
    testReq(post("http://amora.center/sparql-update", q)) {
      status === StatusCodes.OK
    }
    testReq((post("http://amora.center/sparql", """
      prefix p:<http://amora.center/kb/amora/Schema/Project/>
      prefix a:<http://amora.center/kb/amora/Schema/Artifact/>
      select distinct ?name where {
        [a a:] a:owner [p:name ?name] .
      }
    """, header = Accept(CustomContentTypes.`application/sparql-results+json`)))) {
      status === StatusCodes.OK
      resultSetAsData(respAsResultSet) === Seq(
          Seq(Data("name", "p")))
    }
  }

  @Test
  def the_owner_of_a_project_does_not_exist(): Unit = {
    val q = Schema.mkSparqlUpdate(Seq(Project("p")))
    testReq(post("http://amora.center/sparql-update", q)) {
      status === StatusCodes.OK
    }
    testReq((post("http://amora.center/sparql", """
      prefix p:<http://amora.center/kb/amora/Schema/Project/>
      select ?owner where {
        [a p:] p:owner ?owner .
      }
    """, header = Accept(CustomContentTypes.`application/sparql-results+json`)))) {
      status === StatusCodes.OK
      resultSetAsData(respAsResultSet) === Seq()
    }
  }

  @Test
  def the_owner_of_an_artifact_is_a_project(): Unit = {
    val a = Artifact(Project("p"), "o", "n", "v1")
    val q = Schema.mkSparqlUpdate(Seq(a))
    testReq(post("http://amora.center/sparql-update", q)) {
      status === StatusCodes.OK
    }
    testReq((post("http://amora.center/sparql", """
      prefix a:<http://amora.center/kb/amora/Schema/Artifact/>
      prefix rdfs:<http://www.w3.org/2000/01/rdf-schema#>
      select ?tpe where {
        [a a:] a:owner [a ?tpe] .
        filter not exists {
          ?sub rdfs:subClassOf ?tpe .
          filter (?sub != ?tpe)
        }
      }
    """, header = Accept(CustomContentTypes.`application/sparql-results+json`)))) {
      status === StatusCodes.OK
      resultSetAsData(respAsResultSet) === Seq(
          Seq(Data("tpe", "http://amora.center/kb/amora/Schema/Project/")))
    }
  }

  @Test
  def add_single_file(): Unit = {
    indexData(Artifact(Project("p"), "o", "n", "v1"),
      "A.scala" → """
        package pkg
      """)
    testReq((post("http://amora.center/sparql", """
      prefix f:<http://amora.center/kb/amora/Schema/File/>
      select ?name where {
        [a f:] f:name ?name .
      }
    """, header = Accept(CustomContentTypes.`application/sparql-results+json`)))) {
      status === StatusCodes.OK
      resultSetAsData(respAsResultSet) === Seq(
          Seq(Data("name", "A.scala")))
    }
  }

  @Test
  def add_multiple_files(): Unit = {
    indexData(Artifact(Project("p"), "o", "n", "v1"),
      "A.scala" → """
        package pkg
      """,
      "B.scala" → """
        package pkg
      """)
    testReq((post("http://amora.center/sparql", """
      prefix f:<http://amora.center/kb/amora/Schema/File/>
      select ?name where {
        [a f:] f:name ?name .
      }
      order by ?name
    """, header = Accept(CustomContentTypes.`application/sparql-results+json`)))) {
      status === StatusCodes.OK
      resultSetAsData(respAsResultSet) === Seq(
          Seq(Data("name", "A.scala")),
          Seq(Data("name", "B.scala")))
    }
  }

  @Test
  def files_with_same_name_can_belong_to_different_artifacts(): Unit = {
    indexData(Artifact(Project("p1"), "o1", "n1", "v1"),
      "A.scala" → """
        package pkg
      """)
    indexData(Artifact(Project("p2"), "o2", "n2", "v2"),
      "A.scala" → """
        package pkg
      """)
    testReq((post("http://amora.center/sparql", """
      prefix p:<http://amora.center/kb/amora/Schema/Package/>
      prefix f:<http://amora.center/kb/amora/Schema/File/>
      prefix a:<http://amora.center/kb/amora/Schema/Artifact/>
      prefix amora:<http://amora.center/kb/amora/Schema/>
      select ?name ?version where {
        [a f:] amora:owner* [a a:; a:version ?version]; amora:name ?name .
      }
      order by ?name ?version
    """, header = Accept(CustomContentTypes.`application/sparql-results+json`)))) {
      status === StatusCodes.OK
      resultSetAsData(respAsResultSet) === Seq(
          Seq(Data("name", "A.scala"), Data("version", "v1")),
          Seq(Data("name", "A.scala"), Data("version", "v2")))
    }
  }

  @Test
  def artifacts_with_same_name_can_belong_to_different_projects(): Unit = {
    val a1 = Artifact(Project("p1"), "o", "n", "v")
    val a2 = Artifact(Project("p2"), "o", "n", "v")
    val p1 = Package("pkg1", a1)
    val p2 = Package("pkg2", a2)
    val q = Schema.mkSparqlUpdate(Seq(p1, p2))
    testReq(post("http://amora.center/sparql-update", q)) {
      status === StatusCodes.OK
    }
    testReq((post("http://amora.center/sparql", """
      prefix pkg:<http://amora.center/kb/amora/Schema/Package/>
      prefix p:<http://amora.center/kb/amora/Schema/Project/>
      prefix amora:<http://amora.center/kb/amora/Schema/>
      select ?pname ?pkgname where {
        [a pkg:] amora:owner* [a p:; amora:name ?pname]; amora:name ?pkgname .
      }
      order by ?pname ?pkgname
    """, header = Accept(CustomContentTypes.`application/sparql-results+json`)))) {
      status === StatusCodes.OK
      resultSetAsData(respAsResultSet) === Seq(
          Seq(Data("pname", "p1"), Data("pkgname", "pkg1")),
          Seq(Data("pname", "p2"), Data("pkgname", "pkg2")))
    }
  }

  @Test
  def add_single_package(): Unit = {
    indexData(Artifact(Project("p"), "o", "n", "v1"),
      "A.scala" → """
        package pkg
      """)
    testReq((post("http://amora.center/sparql", """
      prefix p:<http://amora.center/kb/amora/Schema/Package/>
      select ?name where {
        [a p:] p:name ?name .
      }
    """, header = Accept(CustomContentTypes.`application/sparql-results+json`)))) {
      status === StatusCodes.OK
      resultSetAsData(respAsResultSet) === Seq(
          Seq(Data("name", "pkg")))
    }
  }

  @Test
  def the_owner_of_the_top_package_is_an_artifact(): Unit = {
    indexData(Artifact(Project("p"), "o", "n", "v1"),
      "A.scala" → """
        package pkg
      """)
    testReq((post("http://amora.center/sparql", """
      prefix p:<http://amora.center/kb/amora/Schema/Package/>
      prefix rdfs:<http://www.w3.org/2000/01/rdf-schema#>
      select ?tpe where {
        [a p:] p:owner [a ?tpe] .
        filter not exists {
          ?sub rdfs:subClassOf ?tpe .
          filter (?sub != ?tpe)
        }
      }
    """, header = Accept(CustomContentTypes.`application/sparql-results+json`)))) {
      status === StatusCodes.OK
      resultSetAsData(respAsResultSet) === Seq(
          Seq(Data("tpe", "http://amora.center/kb/amora/Schema/Artifact/")))
    }
  }

  @Test
  def the_owner_of_a_non_top_package_is_a_package(): Unit = {
    indexData(Artifact(Project("p"), "o", "n", "v1"),
      "A.scala" → """
        package pkg.inner
      """)
    testReq((post("http://amora.center/sparql", """
      prefix p:<http://amora.center/kb/amora/Schema/Package/>
      prefix rdfs:<http://www.w3.org/2000/01/rdf-schema#>
      select ?name ?tpe where {
        [p:owner [a p:]] p:name ?name ; a ?tpe .
        filter not exists {
          ?sub rdfs:subClassOf ?tpe .
          filter (?sub != ?tpe)
        }
      }
      order by ?name ?tpe
    """, header = Accept(CustomContentTypes.`application/sparql-results+json`)))) {
      status === StatusCodes.OK
      resultSetAsData(respAsResultSet) === Seq(
          Seq(Data("name", "inner"), Data("tpe", "http://amora.center/kb/amora/Schema/Package/")))
    }
  }

  @Test
  def add_single_class(): Unit = {
    indexData(Artifact(Project("p"), "o", "n", "v1"),
      "A.scala" → """
        package pkg
        class A
      """)
    testReq((post("http://amora.center/sparql", """
      prefix c:<http://amora.center/kb/amora/Schema/Class/>
      select ?name where {
        [a c:] c:name ?name .
      }
    """, header = Accept(CustomContentTypes.`application/sparql-results+json`)))) {
      status === StatusCodes.OK
      resultSetAsData(respAsResultSet) === Seq(
          Seq(Data("name", "A")))
    }
  }

  @Test
  def the_owner_of_a_top_level_class_is_a_file(): Unit = {
    indexData(Artifact(Project("p"), "o", "n", "v1"),
      "A.scala" → """
        package pkg
        class A
      """)
    testReq((post("http://amora.center/sparql", """
      prefix c:<http://amora.center/kb/amora/Schema/Class/>
      prefix rdfs:<http://www.w3.org/2000/01/rdf-schema#>
      select ?tpe where {
        [a c:] c:owner [a ?tpe] .
        filter not exists {
          ?sub rdfs:subClassOf ?tpe .
          filter (?sub != ?tpe)
        }
      }
    """, header = Accept(CustomContentTypes.`application/sparql-results+json`)))) {
      status === StatusCodes.OK
      resultSetAsData(respAsResultSet) === Seq(
          Seq(Data("tpe", "http://amora.center/kb/amora/Schema/File/")))
    }
  }

  @Test
  def the_owner_of_a_top_level_class_in_the_default_package_is_a_file(): Unit = {
    indexData(Artifact(Project("p"), "o", "n", "v1"),
      "A.scala" → """
        class A
      """)
    testReq(post("http://amora.center/sparql", """
      prefix c:<http://amora.center/kb/amora/Schema/Class/>
      prefix rdfs:<http://www.w3.org/2000/01/rdf-schema#>
      select ?tpe where {
        [a c:] c:owner [a ?tpe] .
        filter not exists {
          ?sub rdfs:subClassOf ?tpe .
          filter (?sub != ?tpe)
        }
      }
    """, header = Accept(CustomContentTypes.`application/sparql-results+json`))) {
      status === StatusCodes.OK
      resultSetAsData(respAsResultSet) === Seq(
          Seq(Data("tpe", "http://amora.center/kb/amora/Schema/File/")))
    }
  }

  @Test
  def the_owner_of_a_file_is_a_package(): Unit = {
    indexData(Artifact(Project("p"), "o", "n", "v1"),
      "A.scala" → """
        package pkg
      """)
    testReq(post("http://amora.center/sparql", """
      prefix f:<http://amora.center/kb/amora/Schema/File/>
      prefix rdfs:<http://www.w3.org/2000/01/rdf-schema#>
      select ?tpe where {
        [a f:] f:owner [a ?tpe] .
        filter not exists {
          ?sub rdfs:subClassOf ?tpe .
          filter (?sub != ?tpe)
        }
      }
      order by ?tpe
    """, header = Accept(CustomContentTypes.`application/sparql-results+json`))) {
      status === StatusCodes.OK
      resultSetAsData(respAsResultSet) === Seq(
          Seq(Data("tpe", "http://amora.center/kb/amora/Schema/Package/")))
    }
  }

  @Test
  def the_owner_of_a_def_is_a_class(): Unit = {
    indexData(Artifact(Project("p"), "o", "n", "v1"),
      "A.scala" → """
        package pkg
        class A {
          def method = 0
        }
      """)
    testReq(post("http://amora.center/sparql", """
      prefix d:<http://amora.center/kb/amora/Schema/Def/>
      prefix rdfs:<http://www.w3.org/2000/01/rdf-schema#>
      select ?tpe where {
        [a d:] d:name "method" ; d:owner [a ?tpe] .
        filter not exists {
          ?sub rdfs:subClassOf ?tpe .
          filter (?sub != ?tpe)
        }
      }
      order by ?tpe
    """, header = Accept(CustomContentTypes.`application/sparql-results+json`))) {
      status === StatusCodes.OK
      resultSetAsData(respAsResultSet) === Seq(
          Seq(Data("tpe", "http://amora.center/kb/amora/Schema/Class/")))
    }
  }

  @Test
  def the_owner_of_a_ctor_is_a_class(): Unit = {
    indexData(Artifact(Project("p"), "o", "n", "v1"),
      "A.scala" → """
        package pkg
        class A(i: Int)
      """)
    testReq(post("http://amora.center/sparql", """
      prefix d:<http://amora.center/kb/amora/Schema/Def/>
      prefix rdfs:<http://www.w3.org/2000/01/rdf-schema#>
      select ?tpe where {
        [a d:] d:owner [a ?tpe] .
        filter not exists {
          ?sub rdfs:subClassOf ?tpe .
          filter (?sub != ?tpe)
        }
      }
      order by ?tpe
    """, header = Accept(CustomContentTypes.`application/sparql-results+json`))) {
      status === StatusCodes.OK
      resultSetAsData(respAsResultSet) === Seq(
          Seq(Data("tpe", "http://amora.center/kb/amora/Schema/Class/")))
    }
  }

  @Test
  def sparql_construct_post_requests_are_possible(): Unit = {
    testReq(post("http://amora.center/sparql-construct", "construct where { ?s ?p ?o } limit 1")) {
      status === StatusCodes.OK
    }
  }

  @Test
  def sparql_construct_post_request_encods_in_turtle(): Unit = {
    indexData(Artifact(Project("p"), "o", "n", "v1"),
      "A.scala" → """
        trait A
        trait B
      """)
    testReq(post("http://amora.center/sparql-construct", """
      prefix Decl:<http://amora.center/kb/amora/Schema/Decl/>
      construct where {
        ?s a Decl: ; Decl:name ?name .
      }
    """)) {
      status === StatusCodes.OK
      import amora.api._
      modelAsData(respAsModel, sparqlQuery"""
        prefix Decl:<http://amora.center/kb/amora/Schema/Decl/>
        select * where {
          [a Decl:] Decl:name ?name .
        }
        order by ?name
      """) === Seq(
          Seq(Data("name", "A")),
          Seq(Data("name", "B")))
    }
  }

}
