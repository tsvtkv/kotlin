package foo

// CHECK_NOT_CALLED: with_dbz3ex

fun f(x: Int): Int = x * 2

fun Int.f(): Int = this * 3

class A(var value: Int) {
    class object {
        fun f(): Int = 5
    }

    fun f(): Int = value
}

fun doWork(a: A): Int =
        // TODO: check that with second parameter is named f?
        with (a) {
            f(f() + A.f()).f()
        }

fun box(): String {
    assertEquals(90, doWork(A(10)))

    return "OK"
}