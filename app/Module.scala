import com.google.inject.AbstractModule
import repositories.*

class Module extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[AccountRepository]).to(classOf[AccountRepositoryImpl])
    bind(classOf[AuthRepository]).to(classOf[AuthRepositoryImpl])
    bind(classOf[FollowRepository]).to(classOf[FollowRepositoryImpl])
    bind(classOf[MediaRepository]).to(classOf[MediaRepositoryImpl])
    bind(classOf[StatusRepository]).to(classOf[StatusRepositoryImpl])
    bind(classOf[UserRepository]).to(classOf[UserRepositoryImpl])
  }
}
