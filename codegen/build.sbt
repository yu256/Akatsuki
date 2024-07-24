ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.4.2"

lazy val root = (project in file("."))
  .settings(
    name := "codegen"
  )

libraryDependencies ++= Seq(
  "com.typesafe.slick" %% "slick" % "3.5.1",
  "com.typesafe.slick" %% "slick-codegen" % "3.5.1",
  "com.github.tminglei" %% "slick-pg" % "0.22.2",
  "org.postgresql" % "postgresql" % "42.7.3"
)