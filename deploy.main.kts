@file:Repository("https://repo1.maven.org/maven2/") @file:DependsOn("com.hierynomus:sshj:0.27.0")

import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.SFTPClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import java.io.File
import kotlin.system.exitProcess

val branch = System.getenv("BRANCH").substringAfterLast('/')
val client = SSHClient()
client.addHostKeyVerifier(PromiscuousVerifier())

client.connect(when (branch) {
    "v6" -> System.getenv("MAINSERVER_IP")
    else -> {
        error("Unknown branch: $branch")
        exitProcess(1)
    }
}, 22)
client.authPassword("root", when(branch) {
    "v6" -> System.getenv("MAINSERVER_SSH_PASSWORD")
    else -> {
        error("Unknown branch: $branch")
        exitProcess(1)
    }
})
fun findFile(dir: String, regex: Regex): String = File(dir).listFiles()!!.find {
    regex.matches(it.name)
}!!.canonicalPath

val serverFile = findFile("build/libs/", "PlotSquared\\-Bukkit-[0-9\\.]{5}\\-SNAPSHOT\\.jar".toRegex())
val serverDest = "/home/cloud/static/CityBuild-1/plugins/PlotSquared-full.jar"
val sftp: SFTPClient = client.newSFTPClient()
fun del(path: String) = try {
    sftp.rm(path)
} catch (e: Exception) {
    println("Failed to delete $path")
}

fun put(path: String, file: String) = sftp.put(file, path)
del(serverDest)
put(serverDest, serverFile)
client.startSession().exec("screen -S SimpleCloud -X stuff 'leave\\nshutdowngroup CityBuild\\n'")
client.disconnect()

println("Exiting...")
exitProcess(0)
println("Should not be here")
exitProcess(-1)
