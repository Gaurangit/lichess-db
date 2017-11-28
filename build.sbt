scalaVersion := "2.12.4"
name := "lichess-db"
organization := "org.lichess"
version := "1.0"
resolvers += "lila-maven" at "https://raw.githubusercontent.com/ornicar/lila-maven/master"
libraryDependencies += "org.reactivemongo" %% "reactivemongo" % "0.12.6"
libraryDependencies += "org.reactivemongo" %% "reactivemongo-akkastream" % "0.12.6"
libraryDependencies += "org.scalaz" %% "scalaz-core" % "7.2.13"
libraryDependencies += "com.github.ornicar" %% "scalalib" % "6.5"
libraryDependencies += "org.lichess" %% "scalachess" % "7.4"
libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % "2.5.7"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"
libraryDependencies += "joda-time" % "joda-time" % "2.9.7"
