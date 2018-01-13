package org.kotlinacademy.backend

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.pipeline.PipelineContext
import io.ktor.request.receiveOrNull
import io.ktor.request.receiveParameters
import io.ktor.response.respond
import io.ktor.routing.*
import org.kotlinacademy.Endpoints
import org.kotlinacademy.backend.errors.MissingParameterError
import org.kotlinacademy.backend.errors.MissingElementError
import org.kotlinacademy.backend.errors.SecretInvalidError
import org.kotlinacademy.backend.repositories.db.DatabaseRepository
import org.kotlinacademy.backend.repositories.email.EmailRepository
import org.kotlinacademy.backend.repositories.network.NotificationsRepository
import org.kotlinacademy.backend.usecases.*
import org.kotlinacademy.data.*

fun Routing.api() {
    val databaseRepository by DatabaseRepository.lazyGet()
    val emailRepository by EmailRepository.lazyGet()
    val notificationRepository by NotificationsRepository.lazyGet()

    route(Endpoints.news) {
        get {
            val newsList = getAllNews(databaseRepository)
            call.respond(NewsData(newsList))
        }
        put {
            requireSecret()
            val news = receiveObject<News>() ?: return@put
            addOrUpdateNews(news, databaseRepository, notificationRepository, emailRepository)
            call.respond(HttpStatusCode.OK)
        }
    }
    route(Endpoints.feedback) {
        get {
            requireSecret()
            val newsList = getAllFeedback(databaseRepository)
            call.respond(FeedbackData(newsList))
        }
        post {
            val feedback = receiveObject<Feedback>() ?: return@post
            addFeedback(feedback, emailRepository, databaseRepository)
            call.respond(HttpStatusCode.OK)
        }
    }
    route(Endpoints.notification) {
        route(Endpoints.notificationRegister) {
            get {
                requireSecret()
                val tokens = getTokenData(databaseRepository)
                call.respond(tokens)
            }
            post {
                val registerTokenData = receiveObject<FirebaseTokenData>() ?: return@post
                addToken(registerTokenData, databaseRepository)
                call.respond(HttpStatusCode.OK)
            }
        }
        route(Endpoints.notificationSend) {
            post {
                requireSecret()
                val text = receiveObject<String>() ?: return@post
                if (notificationRepository == null) {
                    call.respond(HttpStatusCode.ServiceUnavailable, "No notification repository!")
                    return@post
                }
                val url = "https://blog.kotlin-academy.com/"
                sendNotifications(text, url, databaseRepository, notificationRepository, emailRepository)
                call.respond(HttpStatusCode.OK)
            }
        }
    }
    route(Endpoints.subscription) {
        post {
            val email = getParam("email")
            val emailRepository = emailRepository ?: throw MissingElementError("EmailRepository")
            addSubscription(email, databaseRepository, emailRepository)
            call.respond(HttpStatusCode.OK)
        }
        delete {
            val key = getParam("key")
            removeSubscription(key, databaseRepository)
            call.respond(HttpStatusCode.OK)
        }
    }
    route(Endpoints.sendMailing) {
        post {
            requireSecret()
            val title = getParam("title")
            val message = getParam("message")
            val emailRepository = emailRepository ?: throw MissingElementError("EmailRepository")
            sendMailing(title, message, emailRepository, databaseRepository)
        }
    }
}

private suspend fun PipelineContext<*, ApplicationCall>.getParam(name: String): String {
    return call.receiveParameters().get(name) ?: throw MissingParameterError(name)
}

private suspend fun PipelineContext<*, ApplicationCall>.requireSecret() {
    if (call.request.headers["Secret-hash"] != Config.secretHash) {
        throw SecretInvalidError()
    }
}

private suspend inline fun <reified T : Any> PipelineContext<Unit, ApplicationCall>.receiveObject(): T? {
    val data = call.receiveOrNull<T>()
    if (data == null) {
        call.respond(HttpStatusCode.BadRequest, "Invalid body. Should be ${T::class.simpleName} as JSON.")
        return null
    }
    return data
}