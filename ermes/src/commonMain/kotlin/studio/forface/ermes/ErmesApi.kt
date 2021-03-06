@file:Suppress("FunctionName", "MemberVisibilityCanBePrivate")

package studio.forface.ermes

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import studio.forface.ermes.authenticator.AuthenticationParamsBuilder
import studio.forface.ermes.authenticator.Authenticator
import studio.forface.ermes.authenticator.DefaultAuthenticator
import studio.forface.ermes.calladapters.CallAdapter
import studio.forface.ermes.calladapters.DeferredCallAdapter
import studio.forface.ermes.converters.Converter
import studio.forface.ermes.converters.KotlinSerializationConverter
import studio.forface.ermes.entities.Url
import studio.forface.ermes.exceptions.InvalidUrlException
import studio.forface.ermes.servicefactory.ServiceInstancesManager
import studio.forface.ermes.utils.EMPTY_STRING

/**
 * @author Davide Giuseppe Farella.
 * TODO.
 *
 * @throws InvalidUrlException if the [baseUrl] is not valid
 * @see Url.validateOrThrow
 */
open class ErmesApi(

    /** The [String] representation of the base url */
    baseUrl: String,

    /** The [Authenticator] for authenticate http calls */
    open val authenticator: Authenticator = DefaultAuthenticator,

    /** The [CallAdapter] for adapt http calls */
    open val callAdapter: CallAdapter = DeferredCallAdapter,

    /** The [Converter] for deserialize http call results */
    open val converter: Converter = KotlinSerializationConverter,

    /** The [HttpClient] for the API */
    open val client: HttpClient = HttpClient(),

    /** A [Boolean] representing whether the logging should be enabled */
    open val logging: Boolean = false
) {

    /** An [Url] representing the base url for the API */
    internal val baseUrl = Url( baseUrl ).validateOrThrow()

    /** An instance of [ServiceInstancesManager] for manage instances of Services */
    @PublishedApi
    @Suppress("LeakingThis") // TODO: normally that leak won't be a problem, since no one would have many instances of an Api, but a better dependencies management would be a good choice.
    internal val instancesManager = ServiceInstancesManager(this )

    /** @return the instance of the requested Service [S] */
    inline operator fun <reified S: Any> invoke() = instancesManager<S>()
}


/** A constructor of [ErmesApi] via [ErmesApiBuilder] ( DSL ) */
fun ErmesApi( baseUrl: String = EMPTY_STRING, block: ErmesApiBuilder.() -> Unit ): ErmesApi =
    ErmesApiBuilder( baseUrl ).apply( block ).build()


/** A Builder for create [ErmesApi] via DSL */
class ErmesApiBuilder internal constructor( /** @see ErmesApi.baseUrl */ var baseUrl: String ) {

    /** @see ErmesApi.authenticator */
    var authenticator: Authenticator = DefaultAuthenticator

    /** @see ErmesApi.callAdapter */
    var callAdapter: CallAdapter = DeferredCallAdapter

    /** @see ErmesApi.converter */
    var converter: Converter = KotlinSerializationConverter

    /** @see ErmesApi.client */
    var client: HttpClient = HttpClient()

    /** @see ErmesApi.logging */
    var logging = false

    /** A function for set [authenticator] though DSL style */
    fun authenticator( builder: AuthenticationParamsBuilder ) {
        authenticator = Authenticator( builder )
    }

    /** A function for set [client] through [HttpClient] APIs */
    fun client( block: HttpClientConfig<*>.() -> Unit ) {
        client = HttpClient( block )
    }

    /** A function for set [client] through [HttpClient] APIs */
    fun <T : HttpClientEngineConfig> client(
        engineFactory: HttpClientEngineFactory<T>,
        block: HttpClientConfig<T>.() -> Unit = {}
    ) {
        client = HttpClient( engineFactory, block )
    }

    /** Create an instance of [ErmesApi] */
    fun build() = ErmesApi(
        authenticator = authenticator,
        baseUrl =       baseUrl,
        callAdapter =   callAdapter,
        converter =     converter,
        client =        client,
        logging =       logging
    )
}