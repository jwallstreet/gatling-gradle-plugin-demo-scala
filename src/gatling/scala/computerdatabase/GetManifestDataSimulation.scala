package computerdatabase

import io.circe.generic.auto._
import io.circe.parser
import io.gatling.core.Predef._
import io.gatling.http.Predef._

class GetManifestDataSimulation extends Simulation {
  case class TileSetInfo(
                          style: Int,
                          version: Int
                        )

  var tileSetVersionMap = Map[Int, Int]()
  // Make requests here for versions
  val httpProtocol = http
    .baseUrl("https://manifest.geo.apple.com") // replace with your base URL
    .header("Authorization", "Token 4a9eb85510e0226211e055d3629ce40372283443")

  val getMostRecentTileGroup = scenario("Get Most Recent Tile Group")
    .exec(
      http("Get Tile Groups")
        .get("/api/v1/environments/vector-pine/tilegroups")
        .check(status.is(200))
        .check(jsonPath("$[0].name").saveAs("tileGroupId")))

  val getTileSetAndConvertToMap = scenario("Get Tile Set for Most Recent Tile Group")
    .exec(getMostRecentTileGroup)
    .exec(
      http("Get tile set for most recent tile group")
        .get("/api/v1/environments/vector-pine/tilegroups/${tileGroupId}/tilesets")
        .check(status.is(200))
        .check(bodyString.saveAs("tileSetBodyString")))
    .exec(session => {
      val tileSetBodyString = session("tileSetBodyString").as[String]
      val tileSetList = parser.decode[List[TileSetInfo]](tileSetBodyString).getOrElse(List.empty)
      val tileSetMap = tileSetList.map(tileSet => tileSet.style -> tileSet.version).toMap
      session.set("tileSetMap", tileSetMap)
      //      val tileSetList = parser.parse(tileSetBodyString).getOrElse(Json.Null).as[Seq[Map[String, Int]]].getOrElse(List.empty)
      //      tileSetVersionMap = session("tileSetVersionMap").as[Map[String, String]] ++ tileSetMap
      println("tilesetmap:")
      println(tileSetMap)
      MyExternalVariable.setDataList(tileSetMap)
      session
    })

  val secondScenario = scenario("passing to this scenario")
    .exec(session => {
//      val tileSetMap = session("tileSetMap").as[List[TileSetInfo]]
      println("tilesetmap_2:")
      println(MyExternalVariable.getDataList())
      session
    })

  object MyExternalVariable {
    private var dataList = Map[Int, Int]()

    def getDataList(): Map[Int, Int] = {
      dataList
    }

    def setDataList(data: Map[Int, Int]): Unit = {
      dataList = data
    }
  }

  val mySetup = getTileSetAndConvertToMap.inject(atOnceUsers(1)).protocols(httpProtocol)
  val mySetup2 = secondScenario.inject(atOnceUsers(1)).protocols(httpProtocol)

  setUp(
    mySetup.andThen(mySetup2)
  )

}
