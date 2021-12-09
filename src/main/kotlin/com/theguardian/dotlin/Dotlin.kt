package com.theguardian.dotlin

fun main() {
    val g = digraph("G") {
        "main" { shape = "box"; label = "Main" }
        "main" edgeTo "parse" edgeTo "execute"
        "main".edgeTo("init") {
            label = "Hello world"
            weight = 10
        }
    }
    println(g)
}

fun digraph(name: String, func: DigraphBuilder.() -> Unit): Digraph {
    return DigraphBuilder(name).apply(func).build()
}

class DigraphBuilder(private val name: String) {

    private val vertices = mutableSetOf<Vertex>()
    private val vertexAttributes = mutableMapOf<String, VertexAttributesBuilder>()
    private val edges = mutableListOf<Edge>()

    private fun getOrAddVertex(name: String) =
        vertices.find { it.name == name }
            ?: Vertex(name).also { vertices.add(it) }

    fun build(): Digraph {
        val attrVertices = vertices.map { v ->
            val attrs = vertexAttributes[v.name]
            if (attrs != null) {
                v.copy(attributes = attrs.build())
            } else {
                v
            }
        }
        return Digraph(name, attrVertices.toSet(), edges.toList())
    }

    infix fun String.edgeTo(id: String): String {
        val from = getOrAddVertex(this)
        val to = getOrAddVertex(id)
        vertices.add(from)
        vertices.add(to)
        val edge = Edge(from, to)
        edges.add(edge)
        return id
    }

    fun String.edgeTo(id: String, func: (EdgeAttributeBuilder.() -> Unit)? = null): String {
        val from = getOrAddVertex(this)
        val to = getOrAddVertex(id)
        vertices.add(from)
        vertices.add(to)
        val attrs = func?.let { EdgeAttributeBuilder().apply(it).build() }
        edges.add(Edge(from, to, attrs))
        return id
    }

    operator fun String.invoke(func: VertexAttributesBuilder.() -> Unit) {
        vertexAttributes
            .getOrPut(this) { VertexAttributesBuilder() }
            .apply(func)
    }

    operator fun EdgeSyntax.invoke(func: EdgeAttributeBuilder.() -> Unit) {
        edges.removeIf { it === edge }
        edges.add(edge.copy(attributes = EdgeAttributeBuilder().apply(func).build()))
    }
}

class EdgeSyntax(val edge: Edge)

data class Digraph(
    val name: String,
    val vertices: Set<Vertex>,
    val edge: List<Edge>
) {
    override fun toString(): String {
        return """
            |digraph $name {
            |${vertices.filter { it.hasAttributes }.joinToString("\n").prependIndent("\t")}
            |${edge.joinToString("\n").prependIndent("\t")}
            |}""".trimMargin("|")
    }
}

data class Vertex(
    val name: String,
    val attributes: Attributes? = null
) {
    val hasAttributes: Boolean get() = attributes != null

    override fun toString(): String {
        return "$name $attributes"
    }

    data class Attributes(
        val shape: String? = null,
        val label: String? = null,
    ) {
        override fun toString(): String {
            return mapOf(
                "shape" to shape,
                "label" to label
            ).toAttributeString()
        }
    }
}

class VertexAttributesBuilder {
    var shape: String? = null
    var label: String? = null

    fun build(): Vertex.Attributes {
        return Vertex.Attributes(shape, label)
    }
}

data class Edge(
    val from: Vertex,
    val to: Vertex,
    val attributes: Attributes? = null
) {
    override fun toString(): String {
        val attrs = attributes?.toString() ?: ""
        return "${from.name} -> ${to.name} $attrs".trim()
    }

    data class Attributes(
        val weight: Int? = null,
        val label: String? = null
    ) {
        override fun toString(): String {
            return mapOf(
                "weight" to weight,
                "label" to label
            ).toAttributeString()
        }
    }
}

class EdgeAttributeBuilder {
    var weight: Int? = null
    var label: String? = null

    fun build(): Edge.Attributes {
        return Edge.Attributes(weight, label)
    }
}

fun Map<String, Any?>.toAttributeString(): String {
    return mapNotNull { (k, v) ->
        v?.let { "$k=\"$v\"" }
    }.sorted().joinToString(prefix = "[", separator = ",", postfix = "]")
}