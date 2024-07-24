name := """Akatsuki"""
organization := "com.github.yu256"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "3.4.2"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.1" % Test

libraryDependencies ++= Seq(
  "com.typesafe.slick" %% "slick" % "3.5.1",
  "org.playframework" %% "play-slick" % "6.1.0",
  "org.playframework" %% "play-slick-evolutions" % "6.1.1",
  "com.github.tminglei" %% "slick-pg" % "0.22.2",
  "com.github.tminglei" %% "slick-pg_play-json" % "0.22.2",
  "org.postgresql" % "postgresql" % "42.7.3"
)

libraryDependencies += "org.typelevel" %% "cats-core" % "2.12.0"

libraryDependencies ++= Seq(
  "com.nulab-inc" %% "scala-oauth2-core" % "1.6.0",
  "com.nulab-inc" %% "play2-oauth2-provider" % "2.0.0"
)

libraryDependencies += "org.springframework.security" % "spring-security-web" % "6.3.0"

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.github.yu256.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.github.yu256.binders._"
