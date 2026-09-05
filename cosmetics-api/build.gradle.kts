// What the shop and the backend have to agree on: the shape of a worn cosmetic on the wire.
//
// Compiled against nothing but the platform's messaging module, so neither server platform can
// leak into it - a Bukkit or Velocity type here would make the module unusable on the other
// side, which is the whole reason it is its own module.
dependencies {
    api(libs.platform.messaging)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
