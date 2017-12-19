
import com.datamaps.maps.DataProjection
import com.datamaps.maps.f
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import org.apache.commons.lang.text.StrSubstitutor
import org.testng.annotations.Test
import kotlin.reflect.KCallable
import kotlin.reflect.KClass

/**
 * Created by Щукин on 27.10.2017.
 */
class KotlinTests {

    @Test
    fun workWithMaps() {

        val map = mutableMapOf<String, String>()
        map["xxx"] = "zzz";

        println(map["xxx"])

        val myJson = """
                  user(id: 1) {
                    n
                    age
                    friends {
                      n
                    }
                  }
                }
        """.trimIndent()

        println(myJson)

    }

    @Test
    fun testInfixFunctions() {

        //val e1 =

        val dp = DataProjection("dasd")
                .filter({
                    val e = f("hw") gt 10 or ((
                            f("xxx")
                            ))
                    e and f("bnn")
                })
    }

    @Test
    fun testStrSubstitor() {



        var myStr = "hello {{world}}"
        val map = mutableMapOf<String, String>()
        map.put("world", "hell")
        var s = StrSubstitutor(map, "{{", "}}")



        println(s.replace(myStr))

    }

    @Test
    fun testCorutines() {

        val deferred = async {
            workload(1000)
        }
        deferred.invokeOnCompletion {
            println( deferred.getCompleted())
        }


        Thread.sleep(2000) // wait for 2 seconds


    }

    suspend fun workload(n: Int): Int {
        delay(1000)
        println(n)
        return 6666
    }

    var someVar= 0
    @Test
    fun testKolinReflection() {

        voidd(A::b)
        voidd(A::z)
        void(A::class)
    }

    private fun void(kClass: KClass<*>) {

    }


    public class DProjection(kClass: KClass<*>)
    {

    }

    fun voidd(objectt: KCallable<*>)
    {
        println(objectt.name)
    }


    class A
    {
        var b:B? = null
        var z:B? = null
    }

    class B {
        var name2 = ""
    }
}



