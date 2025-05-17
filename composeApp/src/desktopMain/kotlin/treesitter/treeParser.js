const Parser = require('tree-sitter');
const Kotlin = require('tree-sitter-kotlin');

const code = `
fun greet(name: String): String {
    return "Hello, $name!"
}
`;

const parser = new Parser();
parser.setLanguage(Kotlin);

const tree = parser.parse(code);
console.log(tree.rootNode.toString());