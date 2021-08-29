val scala3Version = "3.0.0"
val javaVersion = "11"
val projectName = "scala3-sandbox"
val projectVersion = "0.1.0"

def commonSettings(projectName: String) = {
  Seq(
      name := projectName,
      version := projectVersion,
      scalaVersion := scala3Version,
      libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % "test",
      scalafmtOnCompile := true,
   ) ++ scalacSettings
}

lazy val scalacSettings = Seq(
  scalacOptions ++= Seq("-deprecation", "-feature", s"-Xtarget:${javaVersion}"),
  javacOptions ++= Seq("-source", javaVersion, "-target", javaVersion)
)

lazy val `scala3-sandbox` = project
  .in(file("."))
  .aggregate(syntax)

lazy val `syntax` = project.in(file("syntax"))
  .settings(
    commonSettings("syntax"),
    libraryDependencies ++= Seq(
      ("org.scalameta" %% "scalameta" % "4.4.27").cross(CrossVersion.for3Use2_13)
    )
  )

val AkkaVersion = "2.6.15"
val AkkaHttpVersion = "10.2.6"
val circeVersion = "0.14.1"

val circeDependencies = Seq(
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
)

val sangriaVersion = "2.1.3"
val sangriaDependencies = Seq(
  ("org.sangria-graphql" %% "sangria" % sangriaVersion).cross(CrossVersion.for3Use2_13),
  "org.sangria-graphql" %% "sangria-circe" % "1.3.2"
)

lazy val `webapp-akka` = project.in(file("webapp_akka"))
  .settings(
    commonSettings("webapp-akka"),
    libraryDependencies ++= circeDependencies ++ Seq(
      "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
      "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
      "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion
    ).map(_.cross(CrossVersion.for3Use2_13)) ++ sangriaDependencies
  )
