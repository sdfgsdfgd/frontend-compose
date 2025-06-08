plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    //    alias(libs.plugins.jetbrainsCompose) apply false todo confirm unnecssry?
    alias(libs.plugins.kotlinJvm)           apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.compose.compiler)    apply false
    alias(libs.plugins.jetbrainsCompose)    apply false  // todo: confirm unnecssry?
    alias(libs.plugins.androidLib)          apply false
    alias(libs.plugins.androidApp)          apply false
    alias(libs.plugins.composeHotReload)    apply false
}