package htm
/**
 * Created with IntelliJ IDEA.
 * User: miao.lin
 * Date: 14-3-12
 * Time: 上午11:25
 * To change this template use File | Settings | File Templates.
 */




import org.junit.Ignore
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification
/**
 * Created with IntelliJ IDEA.
 * User: miao.lin
 * Date: 13-12-20
 * Time: 下午5:06
 * To change this template use File | Settings | File Templates.
 */
@ContextConfiguration(locations = [
"classpath*:/config/spring/common/appcontext-*.xml",
"classpath*:/config/spring/local/appcontext-*.xml",
"classpath*:/config/spring/test/appcontext-*.xml"])
@Ignore
class AbstractSpockTest extends Specification {


}

