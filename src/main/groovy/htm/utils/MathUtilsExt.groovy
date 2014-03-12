package htm.utils

import groovy.transform.CompileStatic

/**
 * Created with IntelliJ IDEA.
 * User: miao.lin
 * Date: 14-3-12
 * Time: 上午11:21
 * To change this template use File | Settings | File Templates.
 */
@CompileStatic
class MathUtilsExt extends MathUtils{
    static public double findMax(double... values) {
        return GroovyCollections.max(values.toList())
    }

}
