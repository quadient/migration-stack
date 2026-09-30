package com.quadient.migration.example.docx.parser

import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.builder.ImageBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.ParagraphBuilder
import com.quadient.migration.shared.ImageOptions
import com.quadient.migration.shared.ImageType
import com.quadient.migration.shared.Size
import groovy.transform.Field
import org.apache.poi.common.usermodel.PictureType
import org.apache.poi.xwpf.usermodel.XWPFPicture
import org.apache.poi.xwpf.usermodel.XWPFPictureData
import org.apache.poi.xwpf.usermodel.XWPFRun

@Field
static Map<Long, String> imageIdByChecksum = [:]
@Field
static int imageCounter = 0

static void resetImageState() {
    imageIdByChecksum = [:]
    imageCounter = 0
}

static boolean hasRunImages(XWPFRun run) {
    return !run.embeddedPictures.isEmpty() || DocxVmlImages.hasImages(run)
}

static void processRunImages(Migration migration, XWPFRun run, String fileName, List<ParagraphBuilder.TextBuilder> textBuilders, Set<String> excludedEmbedIds = Collections.emptySet()) {
    run.getEmbeddedPictures().each { XWPFPicture picture ->
        XWPFPictureData data = picture.getPictureData()
        if (data == null) {
            return
        }
        String embedId = picture.getCTPicture()?.getBlipFill()?.getBlip()?.getEmbed()
        if (embedId && excludedEmbedIds.contains(embedId)) {
            return
        }
        addImageRef(textBuilders, registerImageData(migration, data, fileName, resolveOptions(picture)))
    }

    DocxVmlImages.extract(run).each { VmlImageSource source ->
        String imageId
        if (source.embedId) {
            if (excludedEmbedIds.contains(source.embedId)) {
                return
            }
            XWPFPictureData data = run.document.getPictureDataByID(source.embedId)
            imageId = data == null ? null : registerImageData(migration, data, fileName, source.options)
        } else {
            imageId = registerImageBytes(migration, source.bytes, fileName, source.imageType, source.options, source.checksum)
        }
        addImageRef(textBuilders, imageId)
    }
}

private static void addImageRef(List<ParagraphBuilder.TextBuilder> textBuilders, String imageId) {
    if (imageId != null) {
        textBuilders.add(new ParagraphBuilder.TextBuilder().imageRef(imageId))
    }
}

static String registerImageData(Migration migration, XWPFPictureData data, String fileName, ImageOptions options) {
    Long checksum = data.getChecksum()
    if (checksum != null && imageIdByChecksum.containsKey(checksum)) {
        return imageIdByChecksum[checksum]
    }
    ImageType imageType = toImageType(data.getPictureTypeEnum())
    if (imageType == ImageType.Unknown) {
        println "  Warning: Skipping embedded image with unsupported type: ${data.getPictureTypeEnum()}"
        return null
    }
    return registerImageBytes(migration, data.getData(), fileName, imageType, options, checksum)
}

private static String registerImageBytes(Migration migration, byte[] imageBytes, String fileName, ImageType imageType,
                                         ImageOptions options, Long checksum) {
    if (checksum != null && imageIdByChecksum.containsKey(checksum)) {
        return imageIdByChecksum[checksum]
    }

    String imageId = "${fileName}_img_${++imageCounter}"
    String storagePath = "${imageId}${imageType.extension()}"

    // Storage has both String and byte[] overloads. Keep the declared type so a malformed/empty picture payload
    // cannot make Groovy select neither overload at runtime.
    migration.storage.write(storagePath, imageBytes)

    def imageBuilder = new ImageBuilder(imageId)
            .sourcePath(storagePath)
            .imageType(imageType)
            .originLocations([fileName])

    if (options) {
        imageBuilder.options(options)
    }

    migration.imageRepository.upsert(imageBuilder.build())

    if (checksum != null) {
        imageIdByChecksum[checksum] = imageId
    }
    return imageId
}

private static ImageOptions resolveOptions(XWPFPicture picture) {
    try {
        double widthPt = picture.getWidth()
        double heightPt = picture.getDepth()
        if (widthPt > 0 && heightPt > 0) {
            return new ImageOptions(Size.ofPoints(widthPt), Size.ofPoints(heightPt))
        }
    } catch (Exception ignored) {
        // Picture is missing shape xfrm/ext data - skip sizing rather than fail the whole run.
    }
    return null
}

private static ImageType toImageType(PictureType pictureType) {
    switch (pictureType) {
        case PictureType.PNG:
            return ImageType.Png
        case PictureType.JPEG:
        case PictureType.CMYKJPEG:
            return ImageType.Jpeg
        case PictureType.GIF:
            return ImageType.Gif
        case PictureType.BMP:
        case PictureType.DIB:
            return ImageType.Bmp
        case PictureType.TIFF:
            return ImageType.Tiff
        case PictureType.SVG:
            return ImageType.Svg
        default:
            return ImageType.Unknown
    }
}
