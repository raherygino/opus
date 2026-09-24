package com.gsoft.opus.data.api.dto

import com.gsoft.opus.domain.model.AppNotification
import com.gsoft.opus.domain.model.Arme
import com.gsoft.opus.domain.model.ArmeMunitionsConsommation
import com.gsoft.opus.domain.model.Armement
import com.gsoft.opus.domain.model.ArmementAttachment
import com.gsoft.opus.domain.model.AuthResult
import com.gsoft.opus.domain.model.AffectationMateriel
import com.gsoft.opus.domain.model.AffectationMaterielLigne
import com.gsoft.opus.domain.model.TypeMateriel
import com.gsoft.opus.domain.model.MaterielRoulant
import com.gsoft.opus.domain.model.MaterielRoulantAttachment
import com.gsoft.opus.domain.model.MainCourante
import com.gsoft.opus.domain.model.MainCouranteAttachment
import com.gsoft.opus.domain.model.RassemblementJournalier
import com.gsoft.opus.domain.model.RepartitionSecteur
import com.gsoft.opus.domain.model.EvenementSurvenu
import com.gsoft.opus.domain.model.EvenementSurvenuAttachment
import com.gsoft.opus.domain.model.Activite
import com.gsoft.opus.domain.model.ActiviteAttachment
import com.gsoft.opus.domain.model.DispositifEffectif
import com.gsoft.opus.domain.model.DispositifExceptionnel
import com.gsoft.opus.domain.model.Comportement
import com.gsoft.opus.domain.model.Correspondance
import com.gsoft.opus.domain.model.CorrespondanceAttachment
import com.gsoft.opus.domain.model.DeclarationPerte
import com.gsoft.opus.domain.model.DeclarationPerteAttachment
import com.gsoft.opus.domain.model.Mouvement
import com.gsoft.opus.domain.model.Passation
import com.gsoft.opus.domain.model.PassationAttachment
import com.gsoft.opus.domain.model.TypeArme
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
import com.gsoft.opus.domain.model.PlainteEntree
import com.gsoft.opus.domain.model.PlainteEntreeAttachment
import com.gsoft.opus.domain.model.PlainteEntreeSummary
import com.gsoft.opus.domain.model.PlainteSortie
import com.gsoft.opus.domain.model.PlainteSortieAttachment
import com.gsoft.opus.domain.model.Convocation
import com.gsoft.opus.domain.model.ConvocationAttachment
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
    armeId = armeId,
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

fun TypeArmeDto.toDomain(): TypeArme = TypeArme(
    id = id,
    nom = nom,
    description = description,
    munitionsStock = munitionsStock,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun ArmeDto.toDomain(): Arme = Arme(
    id = id,
    typeArmeId = typeArmeId,
    typeArmeNom = typeArmeNom,
    matricule = matricule,
    munitionsStock = munitionsStock,
    typeArmeMunitionsStock = typeArmeMunitionsStock,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun ArmeMunitionsConsommationDto.toDomain(): ArmeMunitionsConsommation = ArmeMunitionsConsommation(
    id = id,
    armeId = armeId,
    agentId = agentId,
    armementId = armementId,
    quantite = quantite,
    dateConsommation = dateConsommation,
    createdAt = createdAt,
    armeMatricule = armeMatricule,
    typeArmeNom = typeArmeNom,
    agentIm = agentIm,
    agentGrade = agentGrade,
    agentFirstname = agentFirstname,
    agentLastname = agentLastname
)

fun TypeMaterielDto.toDomain(): TypeMateriel = TypeMateriel(
    id = id,
    nom = nom,
    description = description,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun AffectationMaterielLigneDto.toDomain(): AffectationMaterielLigne = AffectationMaterielLigne(
    id = id,
    affectationId = affectationId,
    typeMaterielId = typeMaterielId,
    typeMaterielNom = typeMaterielNom,
    etatEmport = etatEmport,
    etatReintegration = etatReintegration,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun AffectationMaterielDto.toDomain(): AffectationMateriel = AffectationMateriel(
    id = id,
    agentPersonnelId = agentPersonnelId,
    agentIm = agentIm,
    agentGrade = agentGrade,
    agentNom = agentNom,
    datePerception = datePerception,
    heurePerception = heurePerception,
    dateReintegration = dateReintegration,
    heureReintegration = heureReintegration,
    statut = statut,
    observations = observations,
    agentVerifie = agentVerifie != 0,
    agentVerifieAt = agentVerifieAt,
    signatureSvg = signatureSvg,
    createdBy = createdBy,
    lignes = lignes?.map { it.toDomain() } ?: emptyList(),
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun MaterielRoulantDto.toDomain(): MaterielRoulant = MaterielRoulant(
    id = id,
    datePerception = datePerception,
    heurePerception = heurePerception,
    typeMateriel = typeMateriel,
    numeroImmatriculation = numeroImmatriculation,
    descriptionVehicule = descriptionVehicule,
    agentConducteurPersonnelId = agentConducteurPersonnelId,
    agentConducteurIm = agentConducteurIm,
    agentConducteurGrade = agentConducteurGrade,
    agentConducteurNom = agentConducteurNom,
    chefDeBordPersonnelId = chefDeBordPersonnelId,
    chefDeBordIm = chefDeBordIm,
    chefDeBordGrade = chefDeBordGrade,
    chefDeBordNom = chefDeBordNom,
    kilometrageDepart = kilometrageDepart,
    niveauCarburantDepart = niveauCarburantDepart,
    heureReintegration = heureReintegration,
    dateReintegration = dateReintegration,
    kilometrageRetour = kilometrageRetour,
    niveauCarburantRetour = niveauCarburantRetour,
    observationsTechniques = observationsTechniques,
    defaillances = defaillances,
    agentVerifie = agentVerifie == 1,
    agentVerifieAt = agentVerifieAt,
    signatureSvg = signatureSvg,
    statut = statut,
    createdBy = createdBy,
    attachments = attachments?.map { it.toDomain() } ?: emptyList(),
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun MaterielRoulantAttachmentDto.toDomain(): MaterielRoulantAttachment = MaterielRoulantAttachment(
    id = id,
    materielRoulantId = materielRoulantId,
    title = title,
    filename = filename,
    originalFilename = originalFilename,
    mimeType = mimeType,
    fileSize = fileSize,
    createdAt = createdAt
)

fun MainCouranteDto.toDomain(): MainCourante = MainCourante(
    id = id,
    dateEvenement = dateEvenement,
    heureEvenement = heureEvenement,
    categorie = categorie,
    description = description,
    origine = origine,
    createdBy = createdBy,
    createdAt = createdAt,
    updatedAt = updatedAt,
    agentUsername = agentUsername,
    agentPrenoms = agentPrenoms,
    agentNom = agentNom,
    attachments = attachments?.map { it.toDomain() } ?: emptyList()
)

fun MainCouranteAttachmentDto.toDomain(): MainCouranteAttachment = MainCouranteAttachment(
    id = id,
    mainCouranteId = mainCouranteId,
    title = title,
    filename = filename,
    originalFilename = originalFilename,
    mimeType = mimeType,
    fileSize = fileSize,
    createdAt = createdAt
)

// ── Rassemblement journalier ───────────────────────────────────────

fun RassemblementJournalierDto.toDomain(): RassemblementJournalier = RassemblementJournalier(
    id = id,
    dateRassemblement = dateRassemblement,
    heureRassemblement = heureRassemblement,
    brigadeService = brigadeService,
    officierPermanence = officierPermanence,
    inspecteurPermanence = inspecteurPermanence,
    chefPoste = chefPoste,
    instructionsAutorite = instructionsAutorite,
    effectifTheorique = effectifTheorique,
    present = present,
    absent = absent,
    motifAbsence = motifAbsence,
    createdBy = createdBy,
    createdAt = createdAt,
    updatedAt = updatedAt,
    agentUsername = agentUsername,
    agentPrenoms = agentPrenoms,
    agentNom = agentNom,
    repartitions = repartitions?.map { it.toDomain() } ?: emptyList()
)

fun RepartitionSecteurDto.toDomain(): RepartitionSecteur = RepartitionSecteur(
    id = id,
    type = type,
    secteur = secteur,
    effectifEngage = effectifEngage,
    chefElementContact = chefElementContact,
    controleContact = controleContact,
    materielsArmements = materielsArmements,
    missions = missions,
    createdAt = createdAt
)

// ── Évènements survenus ─────────────────────────────────────────

fun EvenementSurvenuDto.toDomain(): EvenementSurvenu = EvenementSurvenu(
    id = id,
    dateEvenement = dateEvenement,
    heureEvenement = heureEvenement,
    typeEvenement = typeEvenement,
    lieuExact = lieuExact,
    auteursPresumes = auteursPresumes,
    victimes = victimes,
    temoins = temoins,
    mesuresPrises = mesuresPrises,
    latitude = latitude,
    longitude = longitude,
    createdBy = createdBy,
    createdAt = createdAt,
    updatedAt = updatedAt,
    agentUsername = agentUsername,
    agentPrenoms = agentPrenoms,
    agentNom = agentNom
)

fun EvenementSurvenuAttachmentDto.toDomain(): EvenementSurvenuAttachment = EvenementSurvenuAttachment(
    id = id,
    evenementId = evenementId,
    title = title,
    filename = filename,
    originalFilename = originalFilename,
    mimeType = mimeType,
    fileSize = fileSize,
    createdAt = createdAt
)

// ── Activités (Service Général) ────────────────────────────────────

fun ActiviteDto.toDomain(): Activite = Activite(
    id = id,
    dateActivite = dateActivite,
    heureActivite = heureActivite,
    patrouilleDiurneMotoriseeItineraire = patrouilleDiurneMotoriseeItineraire,
    patrouilleDiurnePedestreItineraire = patrouilleDiurnePedestreItineraire,
    patrouilleDiurnePorteeItineraire = patrouilleDiurnePorteeItineraire,
    patrouilleNocturneMotoriseeItineraire = patrouilleNocturneMotoriseeItineraire,
    patrouilleNocturnePedestreItineraire = patrouilleNocturnePedestreItineraire,
    patrouilleNocturnePorteeItineraire = patrouilleNocturnePorteeItineraire,
    operationCiblee = operationCiblee,
    faitsConstates = faitsConstates,
    compteRenduHierarchie = compteRenduHierarchie,
    conduiteATenir = conduiteATenir,
    natureIntervention = natureIntervention,
    suitesDonnees = suitesDonnees,
    latitude = latitude,
    longitude = longitude,
    createdBy = createdBy,
    createdAt = createdAt,
    updatedAt = updatedAt,
    agentUsername = agentUsername,
    agentPrenoms = agentPrenoms,
    agentNom = agentNom
)

fun ActiviteAttachmentDto.toDomain(): ActiviteAttachment = ActiviteAttachment(
    id = id,
    activiteId = activiteId,
    title = title,
    filename = filename,
    originalFilename = originalFilename,
    mimeType = mimeType,
    fileSize = fileSize,
    createdAt = createdAt
)

// ── Dispositif exceptionnel (Service Général) ────────────────────────

fun DispositifExceptionnelDto.toDomain(): DispositifExceptionnel = DispositifExceptionnel(
    id = id,
    natureEvenement = natureEvenement,
    dateDebut = dateDebut,
    dateFin = dateFin,
    createdBy = createdBy,
    createdAt = createdAt,
    updatedAt = updatedAt,
    agentUsername = agentUsername,
    agentPrenoms = agentPrenoms,
    agentNom = agentNom,
    effectifs = effectifs?.map { it.toDomain() } ?: emptyList()
)

fun DispositifEffectifDto.toDomain(): DispositifEffectif = DispositifEffectif(
    id = id,
    secteur = secteur,
    chefElementContact = chefElementContact,
    controleContact = controleContact,
    materielsArmements = materielsArmements,
    missions = missions,
    createdAt = createdAt
)


// ── Plainte ENTRÉE / SORTIE ────────────────────────────────────────

fun PlainteEntreeDto.toDomain(): PlainteEntree = PlainteEntree(
    id = id,
    type = type,
    datePlainte = datePlainte,
    numeroDossier = numeroDossier,
    numeroSt = numeroSt,
    opjPersonnelId = opjPersonnelId,
    enqueteurPersonnelId = enqueteurPersonnelId,
    partieCivile = partieCivile,
    miseEnCause = miseEnCause,
    adressePc = adressePc,
    infraction = infraction,
    prejudice = prejudice,
    lieuInfraction = lieuInfraction,
    heureInfraction = heureInfraction,
    observation = observation,
    createdBy = createdBy,
    createdAt = createdAt,
    updatedAt = updatedAt,
    opjPrenoms = opjPrenoms,
    opjNom = opjNom,
    opjGrade = opjGrade,
    opjIm = opjIm,
    enqueteurPrenoms = enqueteurPrenoms,
    enqueteurNom = enqueteurNom,
    enqueteurGrade = enqueteurGrade,
    enqueteurIm = enqueteurIm,
    agentUsername = agentUsername,
    agentPrenoms = agentPrenoms,
    agentNom = agentNom,
    attachments = attachments?.map { it.toDomain() } ?: emptyList()
)

fun PlainteEntreeAttachmentDto.toDomain(): PlainteEntreeAttachment = PlainteEntreeAttachment(
    id = id,
    plainteEntreeId = plainteEntreeId,
    title = title,
    filename = filename,
    originalFilename = originalFilename,
    mimeType = mimeType,
    fileSize = fileSize,
    createdAt = createdAt
)

fun PlainteEntreeSummaryDto.toDomain(): PlainteEntreeSummary = PlainteEntreeSummary(
    id = id,
    type = type,
    numeroDossier = numeroDossier,
    datePlainte = datePlainte,
    partieCivile = partieCivile,
    miseEnCause = miseEnCause,
    infraction = infraction,
    opjPrenoms = opjPrenoms,
    opjNom = opjNom,
    opjGrade = opjGrade
)

fun PlainteSortieDto.toDomain(): PlainteSortie = PlainteSortie(
    id = id,
    plainteEntreeId = plainteEntreeId,
    nature = nature,
    dateSortie = dateSortie,
    numero = numero,
    numeroTtr = numeroTtr,
    nomSubstitut = nomSubstitut,
    dateDeferrement = dateDeferrement,
    observation = observation,
    createdBy = createdBy,
    createdAt = createdAt,
    updatedAt = updatedAt,
    entreeType = entreeType,
    entreeNumeroDossier = entreeNumeroDossier,
    entreeDatePlainte = entreeDatePlainte,
    entreeInfraction = entreeInfraction,
    entreeMiseEnCause = entreeMiseEnCause,
    entreePartieCivile = entreePartieCivile,
    entreeOpjPrenoms = entreeOpjPrenoms,
    entreeOpjNom = entreeOpjNom,
    entreeOpjGrade = entreeOpjGrade,
    agentUsername = agentUsername,
    agentPrenoms = agentPrenoms,
    agentNom = agentNom,
    attachments = attachments?.map { it.toDomain() } ?: emptyList()
)

fun PlainteSortieAttachmentDto.toDomain(): PlainteSortieAttachment = PlainteSortieAttachment(
    id = id,
    plainteSortieId = plainteSortieId,
    title = title,
    filename = filename,
    originalFilename = originalFilename,
    mimeType = mimeType,
    fileSize = fileSize,
    createdAt = createdAt
)

// ========================
// Convocation mappers
// ========================

fun ConvocationDto.toDomain(): Convocation = Convocation(
    id = id,
    type = type,
    dateConvocation = dateConvocation,
    numero = numero,
    nom = nom,
    adresse = adresse,
    infraction = infraction,
    personneAccuseRecu = personneAccuseRecu,
    numeroDossier = numeroDossier,
    observation = observation,
    createdBy = createdBy,
    createdAt = createdAt,
    updatedAt = updatedAt,
    agentUsername = agentUsername,
    agentPrenoms = agentPrenoms,
    agentNom = agentNom,
    attachments = attachments?.map { it.toDomain() } ?: emptyList()
)

fun ConvocationAttachmentDto.toDomain(): ConvocationAttachment = ConvocationAttachment(
    id = id,
    convocationId = convocationId,
    title = title,
    filename = filename,
    originalFilename = originalFilename,
    mimeType = mimeType,
    fileSize = fileSize,
    createdAt = createdAt
)
