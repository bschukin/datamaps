
import com.datamaps.SLA
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import org.testng.annotations.Test
import kotlin.reflect.KCallable
import kotlin.reflect.KClass

/**
 * Created by Щукин on 27.10.2017.
 */
class KotlinTests {


    @Test(invocationCount = 0)
    fun testApi() {



        //вариант с перегрузкой оператора invoke
        SLA.dice {
            service()
            priority()
            contractProduct()
            slice(contractOrg) {
                slice(organisation) {
                    name()
                    fullName()
                    FIAS()
                }
                slice(contract) {
                    conclusionDate()
                    finishDate()
                }
            }
        }

        //вариант с перегрузкой оператора unaryMinus
        SLA.dice {
            +service
            +priority
            contractOrg {
                full()
                organisation {
                    +name
                    +fullName
                    +FIAS
                }
                contract {
                    +conclusionDate
                    +finishDate
                    organisation {
                        +this.contracts
                    }
                }
            }
        }

    }
}


@Test
fun testCorutines() {

    val deferred = async {
        workload(1000)
    }
    deferred.invokeOnCompletion {
        println(deferred.getCompleted())
    }


    Thread.sleep(2000) // wait for 2 seconds


}

suspend fun workload(n: Int): Int {
    delay(1000)
    println(n)
    return 6666
}

var someVar = 0
@Test
fun testKolinReflection() {

    voidd(A::b)
    voidd(A::z)
    void(A::class)
}

private fun void(kClass: KClass<*>) {

}


fun voidd(objectt: KCallable<*>) {
    println(objectt.name)
}


class A {
    var b: B? = null
    var z: B? = null
}

class B {
    var name2 = ""
}




