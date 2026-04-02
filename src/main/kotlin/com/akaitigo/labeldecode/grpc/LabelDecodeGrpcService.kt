package com.akaitigo.labeldecode.grpc

import akaitigo.labeldecode.v1.ClassifyAdditivesRequest
import akaitigo.labeldecode.v1.ClassifyAdditivesResponse
import akaitigo.labeldecode.v1.DetectAllergensRequest
import akaitigo.labeldecode.v1.DetectAllergensResponse
import akaitigo.labeldecode.v1.LabelDecodeServiceGrpc
import akaitigo.labeldecode.v1.ParseLabelRequest
import akaitigo.labeldecode.v1.ParseLabelResponse
import com.akaitigo.labeldecode.model.AllergenType
import com.akaitigo.labeldecode.parser.LabelParser
import io.grpc.Status
import io.grpc.stub.StreamObserver
import io.quarkus.grpc.GrpcService
import io.smallrye.common.annotation.Blocking

private const val MAX_RAW_TEXT_LENGTH = 10_000

@GrpcService
@Blocking
class LabelDecodeGrpcService(
    private val parser: LabelParser,
) : LabelDecodeServiceGrpc.LabelDecodeServiceImplBase() {
  override fun parseLabel(
      request: ParseLabelRequest,
      responseObserver: StreamObserver<ParseLabelResponse>,
  ) {
    val rawText = validateRawText(request.rawText, responseObserver) ?: return

    val parsed = parser.parse(rawText)

    val protoIngredients =
        parsed.ingredients.map { ingredient ->
          akaitigo.labeldecode.v1.Ingredient.newBuilder()
              .setName(ingredient.name)
              .addAllAllergenSources(ingredient.allergenSources)
              .build()
        }

    val protoAdditives =
        parsed.additives.map { additive ->
          akaitigo.labeldecode.v1.Additive.newBuilder()
              .setName(additive.name)
              .setCategory(additive.category)
              .build()
        }

    val protoAllergens =
        parsed.allergens.map { allergen ->
          akaitigo.labeldecode.v1.Allergen.newBuilder()
              .setName(allergen.name)
              .setType(mapAllergenType(allergen.type))
              .setSourceText(allergen.sourceText)
              .build()
        }

    val label =
        akaitigo.labeldecode.v1.ParsedLabel.newBuilder()
            .addAllIngredients(protoIngredients)
            .addAllAdditives(protoAdditives)
            .addAllAllergens(protoAllergens)
            .setOriginalText(parsed.originalText)
            .build()

    val response = ParseLabelResponse.newBuilder().setLabel(label).build()

    responseObserver.onNext(response)
    responseObserver.onCompleted()
  }

  override fun detectAllergens(
      request: DetectAllergensRequest,
      responseObserver: StreamObserver<DetectAllergensResponse>,
  ) {
    val rawText = validateRawText(request.rawText, responseObserver) ?: return

    val allergens = parser.detectAllergens(rawText)

    val protoAllergens =
        allergens.map { allergen ->
          akaitigo.labeldecode.v1.Allergen.newBuilder()
              .setName(allergen.name)
              .setType(mapAllergenType(allergen.type))
              .setSourceText(allergen.sourceText)
              .build()
        }

    val response = DetectAllergensResponse.newBuilder().addAllAllergens(protoAllergens).build()

    responseObserver.onNext(response)
    responseObserver.onCompleted()
  }

  override fun classifyAdditives(
      request: ClassifyAdditivesRequest,
      responseObserver: StreamObserver<ClassifyAdditivesResponse>,
  ) {
    val rawText = validateRawText(request.rawText, responseObserver) ?: return

    val additives = parser.parseAdditivesFromFullText(rawText)

    val protoAdditives =
        additives.map { additive ->
          akaitigo.labeldecode.v1.Additive.newBuilder()
              .setName(additive.name)
              .setCategory(additive.category)
              .build()
        }

    val response = ClassifyAdditivesResponse.newBuilder().addAllAdditives(protoAdditives).build()

    responseObserver.onNext(response)
    responseObserver.onCompleted()
  }

  private fun <T> validateRawText(
      rawText: String,
      responseObserver: StreamObserver<T>,
  ): String? {
    val error =
        when {
          rawText.isBlank() -> {
            "raw_text must not be empty"
          }

          rawText.length > MAX_RAW_TEXT_LENGTH -> {
            "raw_text exceeds maximum length of $MAX_RAW_TEXT_LENGTH"
          }

          else -> {
            null
          }
        }
    if (error != null) {
      responseObserver.onError(
          Status.INVALID_ARGUMENT.withDescription(error).asRuntimeException(),
      )
      return null
    }
    return rawText
  }

  private fun mapAllergenType(type: AllergenType): akaitigo.labeldecode.v1.AllergenType =
      when (type) {
        AllergenType.MANDATORY -> {
          akaitigo.labeldecode.v1.AllergenType.ALLERGEN_TYPE_MANDATORY
        }

        AllergenType.RECOMMENDED -> {
          akaitigo.labeldecode.v1.AllergenType.ALLERGEN_TYPE_RECOMMENDED
        }
      }
}
