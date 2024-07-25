import play.api.Environment
import play.api.db.evolutions.*
import play.api.inject.*

import java.io.{FileInputStream, InputStream}
import javax.inject.*

class EvolutionsModule
    extends SimpleModule(
      bind[EvolutionsConfig].toProvider[DefaultEvolutionsConfigParser],
      bind[EvolutionsReader].to[CustomEvolutionsReader],
      bind[EvolutionsApi].to[DefaultEvolutionsApi],
      bind[ApplicationEvolutions]
        .toProvider[ApplicationEvolutionsProvider]
        .eagerly()
    )

@Singleton
class CustomEvolutionsReader @Inject() (environment: Environment)
    extends ResourceEvolutionsReader {
  override def loadResource(db: String, revision: Int): Option[InputStream] =
    environment
      .getFile(Evolutions.directoryName(db))
      .listFiles
      .find(f =>
        f.getName.endsWith(".sql") && f.getName.startsWith(s"${revision}_")
      )
      .map(FileInputStream(_))
}
