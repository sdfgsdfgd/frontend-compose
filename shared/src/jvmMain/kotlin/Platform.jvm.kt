class JVMPlatform: Platform {
    override val name: String = "Java ${System.getProperty("java.version")} OS Name: ${System.getProperty("os.name")}"
}

actual fun getPlatform(): Platform = JVMPlatform()