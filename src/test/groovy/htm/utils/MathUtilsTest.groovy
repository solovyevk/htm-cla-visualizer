package htm.utils

import htm.AbstractSpockTest
import org.junit.Test

/**
 * Created with IntelliJ IDEA.
 * User: miao.lin
 * Date: 14-3-12
 * Time: 上午11:28
 * To change this template use File | Settings | File Templates.
 */
class MathUtilsTest extends AbstractSpockTest{
    @Test
    void testFindMax(){
        expect:
        MathUtilsExt.findMax(a,b,c,d)==c

        where:
        a       |       b       |       c       |       d
        1.1     |       2.2     |       10.8    |       9.9

    }

}
