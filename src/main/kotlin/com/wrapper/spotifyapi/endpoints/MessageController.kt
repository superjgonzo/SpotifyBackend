package com.wrapper.spotifyapi.endpoints

import com.wrapper.spotify.model_objects.specification.AlbumSimplified
import com.wrapper.spotify.model_objects.specification.Paging
import com.wrapper.spotifyapi.configurations.SpotifyRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.ModelAndView
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException

@RestController
class MessageController @Autowired constructor(
  private val spotifyRepository: SpotifyRepository
) {

  @RequestMapping("/temp2")
  fun searchForAlbums(@RequestParam albumSearch: String): List<Message> = searchAlbumsAsync(albumSearch)

  @RequestMapping("/callback")
  fun callback(@RequestParam code: String): List<Message> {
    spotifyRepository.authorizationCodeAsync(code)
    return searchForAlbums("Madeon")
  }

  @RequestMapping(value = ["/login"], method = [RequestMethod.GET])
  fun login(): ModelAndView?{
    val authorizationResult = spotifyRepository.authorizationCodeURIAsync()
    return ModelAndView("redirect:$authorizationResult")
  }

  fun searchAlbumsAsync(searchQuery: String): List<Message> {
    println(spotifyRepository.spotifyApi().accessToken)

    val searchAlbumsRequest = spotifyRepository.spotifyApi().searchAlbums(searchQuery)
      .build()

    return try {
      val pagingFuture: CompletableFuture<Paging<AlbumSimplified>> = searchAlbumsRequest.executeAsync()

      // Thread free to do other tasks...

      // Example Only. Never block in production code.
      val albumSimplifiedPaging = pagingFuture.join()
      val listOfAlbums = mutableListOf<Message>()

      albumSimplifiedPaging.items.forEach {
        println("album title: " + it.name)
        listOfAlbums.add(Message(
          albumTitle = it.name
        ))
      }

      listOfAlbums
    } catch (e: CompletionException) {
      println("Error: " + e.cause!!.message + " FROM MESSAGE CONTROLLER")
      listOf(Message(albumTitle = "Error: " + e.cause!!.message))
    } catch (e: CancellationException) {
      listOf(Message(albumTitle = "Async operation cancelled."))
    }
  }
}

data class Message(
  val albumTitle: String
)