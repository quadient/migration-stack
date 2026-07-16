//! ---
//! displayName: Acknowledgement Letter
//! category: Parser
//! description: Import Migration objects of Acknowledgement Letter from Source example
//! sourceFormat: Vital example
//! ---
// This script assumes resources in the resources directory
package com.quadient.migration.example.example

import com.quadient.migration.api.Migration
import com.quadient.migration.api.dto.migrationmodel.builder.DocumentObjectBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.ImageBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.ParagraphStyleBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.TextStyleBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.VariableBuilder
import com.quadient.migration.api.dto.migrationmodel.builder.VariableStructureBuilder
import com.quadient.migration.shared.Alignment
import com.quadient.migration.shared.DataType
import com.quadient.migration.shared.DocumentObjectType
import com.quadient.migration.shared.ImageType
import com.quadient.migration.api.dto.migrationmodel.PageOptions
import com.quadient.migration.shared.Size

import static com.quadient.migration.example.common.util.InitMigration.initMigration

Migration migration = initMigration(this.binding)

def headerImageFile = this.class.getClassLoader().getResource('exampleResources/acknowledgementLetterFromSource/header_crop.png')
migration.storage.write("header_crop.png", headerImageFile.bytes)
def headerImage = new ImageBuilder("header_crop")
    .imageType(ImageType.Png)
    .sourcePath("header_crop.png")
    .build()

def additionalResources = ["b.cooke.png", "b.d.fleck.png", "d.parsley.png", "e.leroy.png", "Claims.csv", "Policies.csv"]
for (resource in additionalResources) {
    def resourceFile = this.class.getClassLoader().getResource("exampleResources/acknowledgementLetterFromSource/${resource}")
    migration.icmClient.upload("icm://$resource", resourceFile.bytes)
}

migration.imageRepository.upsert(headerImage)

def signatureImageFile = this.class.getClassLoader().getResource('exampleResources/acknowledgementLetterFromSource/a.madsen.png')
migration.storage.write("a.madsen.png", signatureImageFile.bytes)
def signatureImage = new ImageBuilder("a.madsen")
    .imageType(ImageType.Png)
    .sourcePath("a.madsen.png")
    .build()
migration.imageRepository.upsert(signatureImage)

def policyHolderNameVar = new VariableBuilder("PolicyholderName")
    .dataType(DataType.String)
    .build()
def streetVar = new VariableBuilder("Street")
    .dataType(DataType.String)
    .build()
def cityVar = new VariableBuilder("City")
    .dataType(DataType.String)
    .build()
def stateVar = new VariableBuilder("State")
    .dataType(DataType.String)
    .build()
def zipcodeVar = new VariableBuilder("Zipcode")
    .dataType(DataType.String)
    .build()
def policyNumberVar = new VariableBuilder("PolicyNumber")
    .dataType(DataType.String)
    .build()
def claimsOfficerVar = new VariableBuilder("ClaimsOfficer")
    .dataType(DataType.String)
    .build()
def dateOfLossVar = new VariableBuilder("DateOfLoss")
    .dataType(DataType.String)
    .build()
def claimNumberVar = new VariableBuilder("ClaimNumber")
    .dataType(DataType.String)
    .build()
def claimsOfficerEmailShortVar = new VariableBuilder("ClaimsOfficerEmailShort")
    .dataType(DataType.String)
    .build()
def notificationDateVar = new VariableBuilder("NotificationDate")
    .dataType(DataType.String)
    .build()

for (item in [policyHolderNameVar, streetVar, cityVar, stateVar, zipcodeVar, policyNumberVar, claimsOfficerVar, dateOfLossVar, claimNumberVar, claimsOfficerEmailShortVar, notificationDateVar]) {
    migration.variableRepository.upsert(item)
}

def variablePath = "Data.Claims.Value"
migration.variableStructureRepository.upsert(new VariableStructureBuilder("varStructure")
    .addVariable(policyHolderNameVar.id, variablePath)
    .addVariable(streetVar.id, variablePath)
    .addVariable(cityVar.id, variablePath)
    .addVariable(stateVar.id, variablePath)
    .addVariable(zipcodeVar.id, variablePath)
    .addVariable(policyNumberVar.id, variablePath)
    .addVariable(claimsOfficerVar.id, variablePath)
    .addVariable(dateOfLossVar.id, variablePath)
    .addVariable(claimNumberVar.id, variablePath)
    .addVariable(claimsOfficerEmailShortVar.id, variablePath)
    .addVariable(notificationDateVar.id, variablePath)
    .build())

def boldStyle = new TextStyleBuilder("bold").definition {
    it.fontFamily("Aptos")
    it.size(Size.ofPoints(11))
    it.bold(true)
}.build()
def normalStyle = new TextStyleBuilder("normal").definition {
    it.fontFamily("Aptos")
    it.size(Size.ofPoints(11))
    it.bold(false)
}.build()
migration.textStyleRepository.upsert(boldStyle)
migration.textStyleRepository.upsert(normalStyle)

def justifyLeftParagraphStyle = new ParagraphStyleBuilder("justifyLeft")
    .definition {
        it.alignment(Alignment.JustifyLeft)
        it.spaceAfter(Size.ofMillimeters(3))
    }
    .build()
def normalParagraphStyle = new ParagraphStyleBuilder("normal")
    .definition {
        it.spaceAfter(Size.ofMillimeters(3))
    }
    .build()
migration.paragraphStyleRepository.upsert(justifyLeftParagraphStyle)
migration.paragraphStyleRepository.upsert(normalParagraphStyle)

def addressFlow = new DocumentObjectBuilder("addressFlow", DocumentObjectType.Block)
    .internal(true)
    .paragraph {
        it.styleRef(justifyLeftParagraphStyle)
        it.text {
            it.styleRef(normalStyle)
            it.variableRef(policyHolderNameVar)
        }
    }
    .paragraph {
        it.styleRef(justifyLeftParagraphStyle)
        it.text {
            it.styleRef(normalStyle)
            it.variableRef(streetVar)
        }
    }
    .paragraph {
        it.styleRef(justifyLeftParagraphStyle)
        it.text {
            it.styleRef(normalStyle)
            it.variableRef(cityVar)
            it.string(", ")
            it.variableRef(stateVar)
            it.string(" ")
            it.variableRef(zipcodeVar)
        }
    }
    .build()
migration.documentObjectRepository.upsert(addressFlow)

// def infoTable = new DocumentObjectBuilder("infoTable", DocumentObjectType.Block)
//     .internal(true)
//     .table {
//         it.addRow {
//             it.addCell { it.paragraph { it.text {
//                 it.styleRef(normalStyle)
//                 it.string("Policy holder name")
//             } } }
//             it.addCell { it.paragraph { it.text {
//                 it.styleRef(normalStyle)
//                 it.variableRef(policyHolderNameVar)
//             } } }
//         }
//         it.addRow {
//             it.addCell { it.paragraph { it.text {
//                 it.styleRef(normalStyle)
//                 it.string("Date of loss")
//             } } }
//             it.addCell { it.paragraph { it.text {
//                 it.styleRef(normalStyle)
//                 it.variableRef(dateOfLossVar)
//             } } }
//         }
//         it.addRow {
//             it.addCell { it.paragraph { it.text {
//                 it.styleRef(normalStyle)
//                 it.string("Date of notification")
//             } } }
//             it.addCell { it.paragraph { it.text {
//                 it.styleRef(normalStyle)
//                 it.variableRef(notificationDateVar)
//             } } }
//         }
//         it.addRow {
//             it.addCell { it.paragraph { it.text {
//                 it.styleRef(normalStyle)
//                 it.string("Claim number")
//             } } }
//             it.addCell { it.paragraph { it.text {
//                 it.styleRef(normalStyle)
//                 it.variableRef(claimNumberVar)
//             } } }
//         }
//     }
//     .build()
// migration.documentObjectRepository.upsert(infoTable)

def mainFlow = new DocumentObjectBuilder("mainFlow", DocumentObjectType.Block)
    .internal(true)
    .paragraph {
        it.styleRef(justifyLeftParagraphStyle)
        it.text {
            it.styleRef(boldStyle)
            it.string("Subject: Acknowledgement of Notification of Loss – Policy No. ")
            it.variableRef(policyNumberVar)
        }
    }
    .paragraph {
        it.styleRef(justifyLeftParagraphStyle)
        it.text {
            it.styleRef(normalStyle)
            it.string("Dear ")
        }
        it.text {
            it.styleRef(boldStyle)
            it.variableRef(policyHolderNameVar)
        }
        it.text {
            it.styleRef(normalStyle)
            it.string(",")
        }
    }
    .paragraph {
        it.styleRef(justifyLeftParagraphStyle)
        it.text {
            it.styleRef(normalStyle)
            it.string("We acknowledge receipt of your notification of loss dated ")
        }
        it.text {
            it.styleRef(boldStyle)
            it.variableRef(notificationDateVar)
        }
        it.text {
            it.styleRef(normalStyle)
            it.string(" regarding the incident that occurred on ")
        }
        it.text {
            it.styleRef(boldStyle)
            it.variableRef(dateOfLossVar)
        }
        it.text {
            it.styleRef(normalStyle)
            it.string(", under your policy number ")
        }
        it.text {
            it.styleRef(boldStyle)
            it.variableRef(policyNumberVar)
        }
        it.text {
            it.styleRef(normalStyle)
            it.string(".")
        }
    }
    .paragraph {
        it.styleRef(justifyLeftParagraphStyle)
        it.text {
            it.styleRef(normalStyle)
            it.string("We understand the circumstances described in your letter and appreciate you bringing this matter to our attention promptly. Your claim has been registered under the reference number ")
        }
        it.text {
            it.styleRef(boldStyle)
            it.variableRef(claimNumberVar)
        }
        it.text {
            it.styleRef(normalStyle)
            it.string(".")
        }
    }
//    .paragraph {
//        it.styleRef(justifyLeftParagraphStyle)
//        it.text {
//            it.documentObjectRef(infoTable)
//        }
//    }
    .paragraph {
        it.styleRef(justifyLeftParagraphStyle)
        it.text {
            it.styleRef(normalStyle)
            it.string("Our claims department has initiated the review process, and a claims representative will be assigned to your case shortly. You will be contacted within [e.g., 3–5 business days] to discuss the next steps and any additional documentation that may be required to process your claim.")
        }
    }
    .paragraph {
        it.styleRef(justifyLeftParagraphStyle)
        it.text {
            it.styleRef(normalStyle)
            it.string("In the meantime, if you have any urgent questions or need assistance, please feel free to contact our claims support team via email at ")
        }
        it.text {
            it.styleRef(boldStyle)
            it.variableRef(claimsOfficerEmailShortVar)
        }
        it.text {
            it.styleRef(boldStyle)
            it.string("@vital.com.")
        }
    }
    .paragraph {
        it.styleRef(justifyLeftParagraphStyle)
        it.text {
            it.styleRef(normalStyle)
            it.string("We appreciate your cooperation and look forward to resolving your claim efficiently.")
        }
    }
    .paragraph {
        it.styleRef(normalParagraphStyle)
        it.text {
            it.styleRef(normalStyle)
            it.string("Sincerely,")
            it.string("\n")
            it.imageRef(signatureImage)
            it.string("\n")
        }
        it.text {
            it.styleRef(boldStyle)
            it.variableRef(claimsOfficerVar)
            it.string("\n")
        }
        it.text {
            it.styleRef(normalStyle)
            it.string("Claims Officer")
            it.string("\n")
            it.string("Vital Insurance")
        }
    }
    .build()
migration.documentObjectRepository.upsert(mainFlow)

def page = new DocumentObjectBuilder("page1", DocumentObjectType.Page)
    .options(new PageOptions(Size.ofMillimeters(210), Size.ofMillimeters(297)))
    .area {
        it.position {
            it.left(Size.ofMillimeters(0.53))
            it.top(Size.ofMillimeters(0))
            it.width(Size.ofMillimeters(210))
            it.height(Size.ofMillimeters(28))
        }
            .imageRef(headerImage)
    }
    .area {
        it.position {
            it.left(Size.ofMillimeters(20.47))
            it.top(Size.ofMillimeters(40))
            it.width(Size.ofMillimeters(80))
            it.height(Size.ofMillimeters(30))
        }
            .documentObjectRef(addressFlow)
    }
    .area {
        it.position {
            it.left(Size.ofMillimeters(20.21))
            it.top(Size.ofMillimeters(80.26))
            it.width(Size.ofMillimeters(160))
            it.height(Size.ofMillimeters(190))
        }
            .documentObjectRef(mainFlow)
    }
    .build()
migration.documentObjectRepository.upsert(page)

def template = new DocumentObjectBuilder("AcknowledgementLetterFromSource", DocumentObjectType.Template)
    .documentObjectRef(page)
    .build()
migration.documentObjectRepository.upsert(template)

