package com.akaitigo.labeldecode.grpc

import akaitigo.labeldecode.v1.LabelDecodeServiceGrpc
import akaitigo.labeldecode.v1.LabelDecodeServiceOuterClass
import akaitigo.labeldecode.v1.LabelDecodeServiceOuterClass.ClassifyAdditivesRequest
import akaitigo.labeldecode.v1.LabelDecodeServiceOuterClass.ClassifyAdditivesResponse
import akaitigo.labeldecode.v1.LabelDecodeServiceOuterClass.DetectAllergensRequest
import akaitigo.labeldecode.v1.LabelDecodeServiceOuterClass.DetectAllergensResponse
import akaitigo.labeldecode.v1.LabelDecodeServiceOuterClass.ParseLabelRequest
import akaitigo.labeldecode.v1.LabelDecodeServiceOuterClass.ParseLabelResponse
import com.akaitigo.labeldecode.model.AllergenType
import com.akaitigo.labeldecode.parser.LabelParser
import io.grpc.Status
import io.grpc.stub.StreamObserver
import io.quarkus.grpc.GrpcService

@GrpcService
class LabelDecodeGrpcService(
    private val parser: LabelParser,
) : LabelDecodeServiceGrpc.LabelDecodeServiceImplBase() {
    override fun parseLabel(
        request: ParseLabelRequest,
        responseObserver: StreamObserver<ParseLabelResponse>,
    ) {
        val rawText = request.rawText
        if (rawText.isBlank()) {
            responseObserver.onError(
                Status.INVALID_ARGUMENT
                    .withDescription("raw_text must not be empty")
                    .asRuntimeException(),
            )
            return
        }

        val parsed = parser.parse(rawText)

        val protoIngredients =
            parsed.ingredients.map { ingredient ->
                LabelDecodeServiceOuterClass.Ingredient
                    .newBuilder()
                    .setName(ingredient.name)
                    .addAllAllergenSources(ingredient.allergenSources)
                    .build()
            }

        val protoAdditives =
            parsed.additives.map { additive ->
                LabelDecodeServiceOuterClass.Additive
                    .newBuilder()
                    .setName(additive.name)
                    .setCategory(additive.category)
                    .build()
            }

        val protoAllergens =
            parsed.allergens.map { allergen ->
                LabelDecodeServiceOuterClass.Allergen
                    .newBuilder()
                    .setName(allergen.name)
                    .setType(mapAllergenType(allergen.type))
                    .setSourceText(allergen.sourceText)
                    .build()
            }

        val label =
            LabelDecodeServiceOuterClass.ParsedLabel
                .newBuilder()
                .addAllIngredients(protoIngredients)
                .addAllAdditives(protoAdditives)
                .addAllAllergens(protoAllergens)
                .setOriginalText(parsed.originalText)
                .build()

        val response =
            ParseLabelResponse
                .newBuilder()
                .setLabel(label)
                .build()

        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    override fun detectAllergens(
        request: DetectAllergensRequest,
        responseObserver: StreamObserver<DetectAllergensResponse>,
    ) {
        val rawText = request.rawText
        if (rawText.isBlank()) {
            responseObserver.onError(
                Status.INVALID_ARGUMENT
                    .withDescription("raw_text must not be empty")
                    .asRuntimeException(),
            )
            return
        }

        val allergens = parser.detectAllergens(rawText)

        val protoAllergens =
            allergens.map { allergen ->
                LabelDecodeServiceOuterClass.Allergen
                    .newBuilder()
                    .setName(allergen.name)
                    .setType(mapAllergenType(allergen.type))
                    .setSourceText(allergen.sourceText)
                    .build()
            }

        val response =
            DetectAllergensResponse
                .newBuilder()
                .addAllAllergens(protoAllergens)
                .build()

        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    override fun classifyAdditives(
        request: ClassifyAdditivesRequest,
        responseObserver: StreamObserver<ClassifyAdditivesResponse>,
    ) {
        val rawText = request.rawText
        if (rawText.isBlank()) {
            responseObserver.onError(
                Status.INVALID_ARGUMENT
                    .withDescription("raw_text must not be empty")
                    .asRuntimeException(),
            )
            return
        }

        val parsed = parser.parse(rawText)

        val protoAdditives =
            parsed.additives.map { additive ->
                LabelDecodeServiceOuterClass.Additive
                    .newBuilder()
                    .setName(additive.name)
                    .setCategory(additive.category)
                    .build()
            }

        val response =
            ClassifyAdditivesResponse
                .newBuilder()
                .addAllAdditives(protoAdditives)
                .build()

        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    private fun mapAllergenType(type: AllergenType): LabelDecodeServiceOuterClass.AllergenType =
        when (type) {
            AllergenType.MANDATORY -> LabelDecodeServiceOuterClass.AllergenType.ALLERGEN_TYPE_MANDATORY
            AllergenType.RECOMMENDED -> LabelDecodeServiceOuterClass.AllergenType.ALLERGEN_TYPE_RECOMMENDED
        }
}
