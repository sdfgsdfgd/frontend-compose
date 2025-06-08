//package treesitter
//
//import io.github.treesitter.ktreesitter.Language
//import io.github.treesitter.ktreesitter.Parser
//
//
//// xx  - --- - --  - --  ABANDONED  - - - - -- -   -  //  NOT WORKING, PROB NEEDS GRAMMARS, WHATEVER
//// TODO: Link to the PR to the kotlin-treesitter repo   `Issue`   ( lol )
//fun demo() {
//    val code = """
//        const greet = (name) => {
//            return `Hello !`;
//        };
//    """.trimIndent()
//
//
//
//
////    val language = Language("tree-sitter-javascript")
//    val language = Language("javascript")
////    val jsLanguage = Language(NativeUtils.loadLibrary("tree-sitter-javascript")) // Adjust path if needed
//
//    val parser = Parser(language)
//    val tree = parser.parse("fun main() {}")
//    val rootNode = tree.rootNode
//
//    print("ldjskfghsjfdgh")
//    assert(rootNode.type == "source_file")
//    assert(rootNode.startPoint.column.toInt() == 0)
//    assert(rootNode.endPoint.column.toInt() == 13)
//
//    println("Root node: ${rootNode.type}")
//    // Now recurse through the entire rootNode
//    fun recurse(node: io.github.treesitter.ktreesitter.Node, depth: Int = 0) {
//        println("  ".repeat(depth) + node.type)
//        for (i in 0 until node.childCount.toInt()) {
//            node.child(i.toUInt())?.let { recurse(it, depth + 1) }
//        }
//    }
//
//    recurse(rootNode)
//
//    val tree2 = parser.parse(code)
//    println(tree2.rootNode.toString())
//}
//
//fun main() {
//    demo()
//}
//
//
//
