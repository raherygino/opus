package com.gsoft.opus.data.api.dto

import com.gsoft.opus.domain.model.AppNotification
import com.gsoft.opus.domain.model.Armement
import com.gsoft.opus.domain.model.ArmementAttachment
import com.gsoft.opus.domain.model.AuthResult
import com.gsoft.opus.domain.model.Comportement
import com.gsoft.opus.domain.model.Correspondance
import com.gsoft.opus.domain.model.CorrespondanceAttachment
import com.gsoft.opus.domain.model.DeclarationPerte
import com.gsoft.opus.domain.model.DeclarationPerteAttachment
import com.gsoft.opus.domain.model.Mouvement
import com.gsoft.opus.domain.model.Passation
import com.gsoft.opus.domain.model.PassationAttachment
import com.gsoft.opus.domain.model.VerifiedIdentity
import com.gsoft.opus.domain.model.QrAuthDeviceType
import com.gsoft.opus.domain.model.QrAuthRequester
import com.gsoft.opus.domain.model.QrAuthRequestInfo
import com.gsoft.opus.domain.model.QrAuthScanResult
import com.gsoft.opus.domain.model.QrAuthStatus
import com.gsoft.opus.domain.model.QrAuthStatusResult
import com.gsoft.opus.domain.model.MouvementAttachment
import com.gsoft.opus.domain.model.Permission
import com.gsoft.opus.domain.model.Personnel
import com.gsoft.opus.domain.model.PersonnelAttachment
import com.gsoft.opus.domain.model.User

fun UserDto.toDomain(): User = User(
    id = id,
    username = username,
    roleId = roleId,
    roleCode = roleCode,
    roleName = roleName,
    personnelId = personnelId,
    isActive = (isActive ?: 1) == 1,
    permissions = permissions?.map { it.toDomain() } ?: emptyList(),
    firstName = firstname,
    lastName = lastname,
    photo = photo,
    grade = grade,
    affectation = affectation
)

fun PermissionDto.toDomain(): Permission = Permission(
    id = id,
    module = module ?: "",
    canView = (canView ?: 0) == 1,
    canCreate = (canCreate ?: 0) == 1,
    canEdit = (canEdit ?: 0) == 1,
    canDelete = (canDelete ?: 0) == 1,
    canExport = (canExport ?: 0) == 1
)

fun LoginResponseDto.toDomain(): AuthResult = AuthResult(
    accessToken = accessToken,
    refreshToken = refreshToken,
    user = user.toDomain()
)

fun NotificationDto.toDomain(): AppNotification = AppNotification(
    id = id,
    title = title,
    message = message,
    link = link,
    type = type,
    service = service,
    isRead = isRead == 1,
    createdAt = createdAt,
    personnelId = personnelId,
    personnelIm = personnelIm,
    personnelNom = personnelNom,
    personnelPrenoms = personnelPrenoms,
    personnelGrade = personnelGrade,
    personnelPhoto = personnelPhoto,
    createdByUsername = createdByUsername,
    createdByFirstname = createdByFirstname,
    createdByPhoto = createdByPhoto,
    createdByPersonnelId = createdByPersonnelId
)

fun PersonnelDto.toDomain(): Personnel = Personnel(
    id = id,
    im = im,
    grade = grade,
    lastname = lastname,
    firstname = firstname,
    affectation = affectation,
    phone = phone,
    address = address,
    photo = photo,
    thumbnail = thumbnail,
    signature = signature,
    signatureSvg = signatureSvg,
    status = status,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isAdminProfile = isAdminProfile ?: false,
    hasCodeSecret = hasCodeSecret ?: false
)

fun PersonnelAttachmentDto.toDomain(): PersonnelAttachment = PersonnelAttachment(
    id = id,
    personnelId = personnelId,
    title = title,
    filename = filename,
    originalFilename = originalFilename,
    mimeType = mimeType,
    fileSize = fileSize,
    createdAt = createdAt
)

fun MouvementDto.toDomain(): Mouvement = Mouvement(
    id = id,
    personnelId = personnelId,
    im = im,
    grade = grade,
    service = service,
    nom = nom,
    prenoms = prenoms,
    typeMouvement = typeMouvement,
    dateDepart = dateDepart,
    days = days,
    dateRetour = dateRetour,
    retour = retour,
    createdAt = createdAt
)

fun MouvementAttachmentDto.toDomain(): MouvementAttachment = MouvementAttachment(
    id = id,
    mouvementId = mouvementId,
    title = title,
    filename = filename,
    originalFilename = originalFilename,
    mimeType = mimeType,
    fileSize = fileSize,
    createdAt = createdAt
)

fun ComportementDto.toDomain(): Comportement = Comportement(
    id = id,
    personnelId = personnelId,
    im = im,
    grade = grade,
    service = service,
    nom = nom,
    prenoms = prenoms,
    type = type,
    dateComportement = dateComportement,
    motif = motif,
    decision = decision,
    status = status,
    confirmedBy = confirmedBy,
    confirmedAt = confirmedAt,
    rejectedReason = rejectedReason,
    confirmedByUsername = confirmedByUsername,
    createdBy = createdBy,
    createdByUsername = createdByUsername,
    createdAt = createdAt
)

fun CorrespondanceDto.toDomain(): Correspondance = Correspondance(
    id = id,
    dateCorrespondance = dateCorrespondance,
    heureEnregistrement = heureEnregistrement,
    sens = sens,
    reference = reference,
    emetteurDestinataire = emetteurDestinataire,
    objet = objet,
    statut = statut,
    createdBy = createdBy,
    createdAt = createdAt,
    updatedAt = updatedAt,
    agentUsername = agentUsername,
    agentPrenoms = agentPrenoms,
    agentNom = agentNom
)

fun DeclarationPerteDto.toDomain(): DeclarationPerte = DeclarationPerte(
    id = id,
    dateDeclaration = dateDeclaration,
    heureDeclaration = heureDeclaration,
    identiteDeclarant = identiteDeclarant,
    natureObjet = natureObjet,
    descriptionObjet = descriptionObjet,
    datePerte = datePerte,
    lieuPerte = lieuPerte,
    numeroAttestation = numeroAttestation,
    nomAgent = nomAgent,
    createdBy = createdBy,
    createdAt = createdAt,
    updatedAt = updatedAt,
    agentUsername = agentUsername,
    agentPrenoms = agentPrenoms,
    agentNom = agentNom
)

fun CorrespondanceAttachmentDto.toDomain(): CorrespondanceAttachment = CorrespondanceAttachment(
    id = id,
    correspondanceId = correspondanceId,
    title = title,
    filename = filename,
    originalFilename = originalFilename,
    mimeType = mimeType,
    fileSize = fileSize,
    createdAt = createdAt
)

fun DeclarationPerteAttachmentDto.toDomain(): DeclarationPerteAttachment = DeclarationPerteAttachment(
    id = id,
    declarationId = declarationId,
    title = title,
    filename = filename,
    originalFilename = originalFilename,
    mimeType = mimeType,
    fileSize = fileSize,
    createdAt = createdAt
)

fun QrAuthRequesterDto.toDomain(): QrAuthRequester = QrAuthRequester(
    username = username,
    firstname = firstname,
    lastname = lastname,
    roleCode = roleCode,
    roleName = roleName
)

fun QrAuthRequestResponseDto.toDomain(): QrAuthRequestInfo = QrAuthRequestInfo(
    requestCode = requestCode,
    deviceType = QrAuthDeviceType.fromValue(deviceType),
    deviceName = deviceName,
    expiresAt = expiresAt,
    ttlSeconds = ttlSeconds
)

fun QrAuthScanResponseDto.toDomain(): QrAuthScanResult = QrAuthScanResult(
    requestCode = requestCode,
    deviceType = QrAuthDeviceType.fromValue(deviceType),
    deviceName = deviceName,
    requester = requester?.toDomain(),
    expiresAt = expiresAt
)

fun QrAuthStatusResponseDto.toDomain(): QrAuthStatusResult = QrAuthStatusResult(
    requestCode = requestCode,
    deviceType = QrAuthDeviceType.fromValue(deviceType),
    deviceName = deviceName,
    status = QrAuthStatus.fromValue(status),
    expiresAt = expiresAt,
    scannedAt = scannedAt,
    resolvedAt = resolvedAt,
    accessToken = accessToken,
    refreshToken = refreshToken,
    user = user?.toDomain()
)

fun PassationDto.toDomain(): Passation = Passation(
    id = id,
    datePassation = datePassation,
    heurePassation = heurePassation,
    chefDescendantUserId = chefDescendantUserId,
    chefDescendantGrade = chefDescendantGrade,
    chefDescendantLastname = chefDescendantLastname,
    chefMontantUserId = chefMontantUserId,
    chefMontantGrade = chefMontantGrade,
    chefMontantLastname = chefMontantLastname,
    instructionsAutorite = instructionsAutorite,
    incidentsSurvenus = incidentsSurvenus,
    createdBy = createdBy,
    createdAt = createdAt,
    updatedAt = updatedAt,
    chefDescendantUsername = chefDescendantUsername,
    chefMontantUsername = chefMontantUsername
)

fun PassationAttachmentDto.toDomain(): PassationAttachment = PassationAttachment(
    id = id,
    passationId = passationId,
    title = title,
    filename = filename,
    originalFilename = originalFilename,
    mimeType = mimeType,
    fileSize = fileSize,
    createdAt = createdAt
)

fun VerifiedIdentityDto.toDomain(): VerifiedIdentity = VerifiedIdentity(
    id = id,
    username = username,
    grade = grade,
    firstname = firstname,
    lastname = lastname
)

fun ArmementDto.toDomain(): Armement = Armement(
    id = id,
    datePerception = datePerception,
    heurePerception = heurePerception,
    agentPreneurPersonnelId = agentPreneurPersonnelId,
    agentPreneurIm = agentPreneurIm,
    agentPreneurGrade = agentPreneurGrade,
    agentPreneurNom = agentPreneurNom,
    typeArme = typeArme,
    matriculeArme = matriculeArme,
    munitions = munitions,
    secteurMission = secteurMission,
    etatPerception = etatPerception,
    agentVerifie = agentVerifie == 1,
    agentVerifieAt = agentVerifieAt,
    signatureSvg = signatureSvg,
    latitude = latitude?.toDoubleOrNull(),
    longitude = longitude?.toDoubleOrNull(),
    heureReintegration = heureReintegration,
    dateReintegration = dateReintegration,
    etatReintegration = etatReintegration,
    munitionsConsommees = munitionsConsommees,
    reintegrationLatitude = reintegrationLatitude?.toDoubleOrNull(),
    reintegrationLongitude = reintegrationLongitude?.toDoubleOrNull(),
    createdBy = createdBy,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun ArmementAttachmentDto.toDomain(): ArmementAttachment = ArmementAttachment(
    id = id,
    armementId = armementId,
    title = title,
    filename = filename,
    originalFilename = originalFilename,
    mimeType = mimeType,
    fileSize = fileSize,
    createdAt = createdAt
)
