# kara

[![build](https://img.shields.io/github/workflow/status/electronicarts/kara/Scala%20CI)](https://github.com/electronicarts/kara/actions)
[![coverage](https://img.shields.io/codecov/c/github/electronicarts/kara)](https://codecov.io/gh/electronicarts/kara)
![status](https://img.shields.io/badge/status-active-brightgreen.svg)
[![maven central](https://maven-badges.herokuapp.com/maven-central/com.ea.kara/kara/badge.svg)](https://search.maven.org/artifact/com.ea.kara/kara)
[![release](https://img.shields.io/github/v/release/electronicarts/kara)](https://github.com/electronicarts/kara/releases)

### Generate [Finagle](https://twitter.github.io/finagle/) HTTP/JSON services and [Swagger UI](https://swagger.io/tools/swagger-ui/) from [Thrift](https://thrift.apache.org/) service definitions.

------

## Purpose

Provide a quick and human-friendly way of discovering and trying out Thrift APIs, without having to compile Thrift clients for your services.
With **kara**, all your Thrift APIs can benefit from Swagger UI, where schema is exposed, and requests can be tried.

And since a HTTP/JSON API is exposed, you don't necessarily have to deal with the Swagger UI to interact with your Thrift services: you can use your favourite tools (e.g. `curl`, [Postman](https://www.postman.com/)) or anything that talks HTTP/JSON, really!

**NOTE**: while extremely useful during development, **kara** is not intended for production use.

## Usage

- Add **kara** as a plugin to the sbt project adding a line containing `addSbtPlugin("com.ea.kara" % "kara" % "0.1.0")` in `project/plugins.sbt`.
- In your project settings in `build.sbt`:
  - configure `karaServices = Seq("fully_qualified_service_1", "fully_qualified_service_2, ...)` to indicate the Thrift services Kara should generate Finagle services and Swagger UI for. Services should be listed in `<JAVA_NAMESPACE>.<SERVICE_NAME>` format.
  - enable the the plugin with `.enablePlugins(Kara)` on the project that lists the Thrift sources and on which `ScroogeSBT` is enabled.

On compilation (`sbt compile`), a Finagle HTTP service named `Http<SERVICE_NAME>` is generated, which takes as input an instance of a [Scrooge](http://twitter.github.io/scrooge/)-generated Thrift service `<SERVICE_NAME>.MethodPerEndpoint`. All is left to do is to instantiate it in your app and bind it to a Finagle server on a port of your choice.

## Example

#### project/plugins.sbt

```scala
addSbtPlugin("com.ea.kara" % "kara" % "0.1.0")
```

#### build.sbt

```scala
// ...

lazy val thrift = project.in(file("thrift"))
  .settings(
    // ...
    karaServices := Seq("path.to.ExampleService1", "path.to.ExampleService2")
  ).enablePlugins(Kara)
  
// ...  
```

#### App.scala

```scala
import com.twitter.finagle.Http

// ...

val thriftSvc: Service1.MethodPerEndpoint = ???
val karaSvc: HttpService1 = new HttpExampleService1(thriftSvc)
Http.server.serve(":8080", karaSvc)

// ...
```

[Scripted tests](./src/sbt-test/kara/) are a great way to see **kara** in action.

## Swagger UI

`kara` v.`0.1.0` employs Swagger UI v.`3.31.1`.

## Testing

**kara** features two modes of testing:
- *Unit tests*, testing code generation logic:
  - `sbt test`
- *E2E tests*, testing plugin functionality via [scripted](https://www.scala-sbt.org/1.x/docs/Testing-sbt-plugins.html) test framework:
  - `sbt scripted`

## Developer

**kara** was born at [EA DICE](https://www.dice.se/).

## Contributing

Before you can contribute, EA must have a Contributor License Agreement (CLA) on file that has been signed by you. You can find the CLA [here](https://electronicarts.na1.echosign.com/public/esignWidget?wid=CBFCIBAA3AAABLblqZhByHRvZqmltGtliuExmuV-WNzlaJGPhbSRg2ufuPsM3P0QmILZjLpkGslg24-UJtek*).

## License

Modified BSD License (3-Clause BSD license). See [LICENSE](./LICENSE).
