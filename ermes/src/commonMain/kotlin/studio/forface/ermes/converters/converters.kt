@file:Suppress("UNCHECKED_CAST", "KDocUnresolvedReference")

package studio.forface.ermes.converters

import io.ktor.client.response.HttpResponse
import io.ktor.client.response.readBytes
import io.ktor.client.response.readText
import kotlinx.serialization.*
import kotlinx.serialization.context.getOrDefault
import kotlinx.serialization.json.Json
import kotlin.reflect.KClass
import kotlin.reflect.KType

/**
 * @author Davide Giuseppe Farella
 * TODO
 */
interface Converter {

    /** @return [Any] created by converting [HttpResponse] to the given [KType] */
    suspend operator fun invoke( response: HttpResponse, kType: KType ) : Any
}

/** A [Converter] that works with Kotlinx Serialization */
object KotlinSerializationConverter : Converter {

    /** @return [Any] created by converting [HttpResponse] to the given [KType], with [Json] */
    @ImplicitReflectionSerializer
    override suspend operator fun invoke( response: HttpResponse, kType: KType ) : Any {
        return when( kType.classifier as KClass<*> ) {
            HttpResponse::class -> response
            ByteArray::class -> response.readBytes()
            String::class -> response.readText()
            List::class -> Json.parseList( kType, response.readText() )
            Map::class -> Json.parseMap( kType, response.readText() )
            else -> Json.parse( kType, response.readText() )
        }
    }

    /** @see kotlinx.serialization.parse */
    @ImplicitReflectionSerializer
    fun StringFormat.parse( kType: KType, str: String ): Any =
        parse( context.getOrDefault( kType.classifier as KClass<*> ), str )

    /** @see kotlinx.serialization.parseList */
    @ImplicitReflectionSerializer
    fun StringFormat.parseList( kType: KType, objects: String ): List<Any> {
        val kClass = kType.arguments.first().type!!.classifier as KClass<*>
        return parse( context.getOrDefault( kClass ).list, objects )
    }

    /** @see kotlinx.serialization.parseMap */
    @ImplicitReflectionSerializer
    fun StringFormat.parseMap( kType: KType, map: String ): Map<Any, Any> {
        val kTypeArgs = kType.arguments

        val kClassK = kTypeArgs[0].type!!.classifier as KClass<*>
        val kClassV = kTypeArgs[1].type!!.classifier as KClass<*>

        val kSerializerK = context.getOrDefault( kClassK ) as KSerializer<Any>
        val kSerializerV = context.getOrDefault( kClassV ) as KSerializer<Any>

        return parse( ( kSerializerK to kSerializerV ).map, map )
    }
}