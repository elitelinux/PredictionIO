package controllers.Itemrec

import io.prediction.commons.settings.Algo

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms.{tuple, number, text, list, boolean, nonEmptyText}
import play.api.libs.json.Json._
import play.api.libs.json._

import controllers.Application.{algos, withUser, algoInfos}

object MahoutParallelALS extends Controller {
  
  def updateSettings(app_id:String, engine_id:String, algo_id:String) = withUser { user => implicit request =>
    /* request payload
     * {"app_id":"app_id1234","engine_id":"engne_id1234","id":"algo_id2","distanceFunc":"consine","viewmoreAction":"4",
     * "override":"latest","viewAction":"3","buyAction":"4","priorCorrelation":"0","dislikeAction":1,"likeAction":"5","virtualCount":"50","dislike":"1"}
     */
    
    val algoSettingForm = Form(tuple(
      "id" -> number, // algoid
      "app_id" -> number,
      "engine_id" -> number,
      "lambda" -> nonEmptyText, // TODO: verifying
      "implicitFeedback" -> boolean,
      "alpha" -> nonEmptyText,
      "numFeatures" -> number,
      "numIterations" -> number,
      "viewParam" -> nonEmptyText, // TODO: verifying 1 - 5 or text "ignore"
      "likeParam" -> nonEmptyText,
      "dislikeParam" -> nonEmptyText,
      "conversionParam" -> nonEmptyText,
      "conflictParam" -> nonEmptyText // TODO: verifying
    ))
    
    algoSettingForm.bindFromRequest.fold(
      formWithError => {
        println(formWithError.errors) // TODO: send back more meaningful message
        val msg = formWithError.errors(0).message + " Update Failed." // extract 1st error message only
        BadRequest(toJson(Map("message" -> toJson(msg))))
      },
      formData => {
        val (id, appId, engineId, 
          lambda, implicitFeedback, alpha, numFeatures, numIterations,
          viewParam, likeParam, dislikeParam, conversionParam, conflictParam) = formData
        
        // get original Algo first
        val optAlgo: Option[Algo] = algos.get(id)
        
        optAlgo map { algo =>
          val updatedAlgo = algo.copy(
            params = algo.params ++ Map("lambda" -> lambda, // NOTE: read-modify-write!
                "implicitFeedback" -> implicitFeedback,
                "alpha" -> alpha,
                "numFeatures" -> numFeatures,
                "numIterations" -> numIterations,
                "viewParam" -> viewParam,
                "likeParam" -> likeParam,
                "dislikeParam" -> dislikeParam,
                "conversionParam" -> conversionParam,
                "conflictParam" -> conflictParam
                )
          )
          
          algos.update(updatedAlgo)
          Ok
        } getOrElse {
          NotFound(toJson(Map("message" -> toJson("Invalid app id, engine id or algo id. Update failed."))))
        }   
      }
    )
    
  }
  
  /* Return default value if nothing has been set */
  def getSettings(app_id:String, engine_id:String, algo_id:String) = withUser { user => implicit request =>
    /*
    Ok(toJson(Map(
      "id" -> toJson("algo_id2"), // engine id
      "app_id" -> toJson("app_id1234"),
      "engine_id" -> toJson("engne_id1234"),
      "distanceFunc" -> toJson("cosine"),
      "virtualCount" -> toJson(50),
      "priorCorrelation" -> toJson(0),
      "viewAction" -> toJson(3),
      "viewmoreAction" -> toJson(4),
      "likeAction" -> toJson(5),
      "dislikeAction" -> toJson(1),
      "buyAction" -> toJson(5),
      "override" -> toJson("latest")
    )))
    */
    
    // TODO: check user owns this app + engine + aglo
    
    // TODO: check algo_id is Int
    val optAlgo: Option[Algo] = algos.get(algo_id.toInt)
    
    optAlgo map { algo =>

      Ok(toJson(Map(
        "id" -> toJson(algo.id),
        "app_id" -> toJson(app_id),
        "engine_id" -> toJson(engine_id)
        ) ++ (algo.params map { case (k,v) => (k, toJson(v.toString))})
      ))
    } getOrElse {
      NotFound(toJson(Map("message" -> toJson("Invalid app id, engine id or algo id."))))
    }
  }
}