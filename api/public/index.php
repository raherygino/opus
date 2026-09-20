<?php

/**
 * OPUS API — Front Controller
 *
 * Pure PHP REST API (no framework)
 */

// Bootstrap
require __DIR__ . '/../config/bootstrap.php';

use App\Middleware\CorsMiddleware;
use App\Router;
use App\Controllers\AuthController;
use App\Controllers\MouvementController;
use App\Controllers\ComportementController;
use App\Controllers\CorrespondanceController;
use App\Controllers\CorrespondanceAttachmentController;
use App\Controllers\DeclarationPerteController;
use App\Controllers\DeclarationPerteAttachmentController;
use App\Controllers\PassationController;
use App\Controllers\PassationAttachmentController;
use App\Controllers\ArmementController;
use App\Controllers\ArmementAttachmentController;
use App\Controllers\ArmeController;
use App\Controllers\TypeArmeController;
use App\Controllers\TypeMaterielController;
use App\Controllers\AffectationMaterielController;
use App\Controllers\MaterielRoulantController;
use App\Controllers\MaterielRoulantAttachmentController;
use App\Controllers\MainCouranteController;
use App\Controllers\MainCouranteAttachmentController;
use App\Controllers\MainCouranteCategorieController;
use App\Controllers\PlainteEntreeController;
use App\Controllers\PlainteEntreeAttachmentController;
use App\Controllers\PlainteSortieController;
use App\Controllers\PlainteSortieAttachmentController;
use App\Controllers\ConvocationController;
use App\Controllers\ConvocationAttachmentController;
use App\Controllers\GardeAVueController;
use App\Controllers\GardeAVueAttachmentController;
use App\Controllers\RequisitionController;
use App\Controllers\RequisitionAttachmentController;
use App\Controllers\PersonneRechercheeController;
use App\Controllers\ObjetSaisiController;
use App\Controllers\ObjetSaisiAttachmentController;
use App\Controllers\ObjetTrouveController;
use App\Controllers\ObjetTrouveAttachmentController;
use App\Controllers\PerquisitionController;
use App\Controllers\PerquisitionAttachmentController;
use App\Controllers\RenseignementPjController;
use App\Controllers\RenseignementPjAttachmentController;
use App\Controllers\MandatController;
use App\Controllers\MandatAttachmentController;
use App\Controllers\ArrestationController;
use App\Controllers\ArrestationAttachmentController;
use App\Controllers\RassemblementJournalierController;
use App\Controllers\MouvementAttachmentController;
use App\Controllers\PersonnelController;
use App\Controllers\PersonnelAttachmentController;
use App\Controllers\RoleController;
use App\Controllers\UserController;
use App\Controllers\NotificationController;
use App\Controllers\DeviceTokenController;
use App\Controllers\AuditLogController;
use App\Controllers\QrAuthController;

// --- CORS ---
CorsMiddleware::handle();

// --- Router ---
$router = new Router();

// ========================
// Auth Routes
// ========================
$router->post('/api/auth/login',    [AuthController::class, 'login']);
$router->post('/api/auth/refresh',  [AuthController::class, 'refresh']);
$router->get('/api/auth/me',        [AuthController::class, 'me']);
$router->put('/api/auth/password',  [AuthController::class, 'password']);
$router->post('/api/auth/verify',   [AuthController::class, 'verify']);
$router->post('/api/auth/photo',    [AuthController::class, 'uploadPhoto']);
$router->delete('/api/auth/photo',    [AuthController::class, 'deletePhoto']);

// ========================
// QR Auth Routes (scan-to-log-in between desktop & phone)
// ========================
$router->post('/api/qr-auth/request',           [QrAuthController::class, 'request']);
$router->get('/api/qr-auth/{code}',              [QrAuthController::class, 'status']);
$router->post('/api/qr-auth/{code}/scan',        [QrAuthController::class, 'scan']);
$router->post('/api/qr-auth/{code}/approve',     [QrAuthController::class, 'approve']);
$router->post('/api/qr-auth/{code}/reject',      [QrAuthController::class, 'reject']);
$router->post('/api/qr-auth/{code}/cancel',      [QrAuthController::class, 'cancel']);

// ========================
// Personnel Routes
// ========================
$router->get('/api/personnel',              [PersonnelController::class, 'index']);
$router->get('/api/personnel/available',    [PersonnelController::class, 'available']);
$router->get('/api/personnel/{id}',         [PersonnelController::class, 'show']);
$router->post('/api/personnel',             [PersonnelController::class, 'store']);
$router->put('/api/personnel/{id}',         [PersonnelController::class, 'update']);
$router->delete('/api/personnel/{id}',      [PersonnelController::class, 'destroy']);

// ========================
// Personnel Photo Routes
// ========================
$router->post('/api/personnel/{id}/photo',     [PersonnelController::class, 'uploadPhoto']);
$router->delete('/api/personnel/{id}/photo',    [PersonnelController::class, 'deletePhoto']);
$router->get('/api/personnel/{id}/photo',      [PersonnelController::class, 'servePhoto']);
$router->get('/api/personnel/{id}/thumbnail',  [PersonnelController::class, 'serveThumbnail']);

// ========================
// Personnel Signature Routes
// ========================
$router->post('/api/personnel/{id}/signature',     [PersonnelController::class, 'uploadSignature']);
$router->post('/api/personnel/{id}/signature/svg',  [PersonnelController::class, 'saveSignatureSvg']);
$router->get('/api/personnel/{id}/signature',      [PersonnelController::class, 'serveSignature']);

// ========================
// Personnel Code Secret Routes (Armement identity verification)
// ========================
$router->post('/api/personnel/{id}/code-secret',          [PersonnelController::class, 'setCodeSecret']);
$router->post('/api/personnel/{id}/verify-code-secret',   [PersonnelController::class, 'verifyCodeSecret']);

// ========================
// Personnel Attachment Routes
// ========================
$router->get('/api/personnel/{id}/attachments',                       [PersonnelAttachmentController::class, 'index']);
$router->post('/api/personnel/{id}/attachments',                      [PersonnelAttachmentController::class, 'store']);
$router->put('/api/personnel/{id}/attachments/{attachId}',            [PersonnelAttachmentController::class, 'update']);
$router->delete('/api/personnel/{id}/attachments/{attachId}',         [PersonnelAttachmentController::class, 'destroy']);
$router->get('/api/personnel/{id}/attachments/{attachId}/download',   [PersonnelAttachmentController::class, 'download']);

// ========================
// Mouvement Routes
// ========================
$router->get('/api/mouvements',           [MouvementController::class, 'index']);
$router->get('/api/mouvements/{id}',      [MouvementController::class, 'show']);
$router->post('/api/mouvements',          [MouvementController::class, 'store']);
$router->put('/api/mouvements/{id}',          [MouvementController::class, 'update']);
$router->put('/api/mouvements/{id}/retour',   [MouvementController::class, 'retour']);
$router->delete('/api/mouvements/{id}',       [MouvementController::class, 'destroy']);

// ========================
// Mouvement Attachment Routes
// ========================
$router->get('/api/mouvements/{id}/attachments',                       [MouvementAttachmentController::class, 'index']);
$router->post('/api/mouvements/{id}/attachments',                      [MouvementAttachmentController::class, 'store']);
$router->put('/api/mouvements/{id}/attachments/{attachId}',            [MouvementAttachmentController::class, 'update']);
$router->delete('/api/mouvements/{id}/attachments/{attachId}',         [MouvementAttachmentController::class, 'destroy']);
$router->get('/api/mouvements/{id}/attachments/{attachId}/download',   [MouvementAttachmentController::class, 'download']);

// ========================
// Comportement Routes
// ========================
$router->get('/api/comportements',               [ComportementController::class, 'index']);
$router->get('/api/comportements/{id}',          [ComportementController::class, 'show']);
$router->post('/api/comportements',              [ComportementController::class, 'store']);
$router->put('/api/comportements/{id}',          [ComportementController::class, 'update']);
$router->put('/api/comportements/{id}/confirm',  [ComportementController::class, 'confirm']);
$router->put('/api/comportements/{id}/reject',   [ComportementController::class, 'reject']);
$router->delete('/api/comportements/{id}',       [ComportementController::class, 'destroy']);

// ========================
// Correspondance Routes
// ========================
$router->get('/api/correspondances',           [CorrespondanceController::class, 'index']);
$router->get('/api/correspondances/{id}',      [CorrespondanceController::class, 'show']);
$router->post('/api/correspondances',          [CorrespondanceController::class, 'store']);
$router->put('/api/correspondances/{id}',      [CorrespondanceController::class, 'update']);
$router->delete('/api/correspondances/{id}',   [CorrespondanceController::class, 'destroy']);

// ========================
// Correspondance Attachment Routes
// ========================
$router->get('/api/correspondances/{id}/attachments',                       [CorrespondanceAttachmentController::class, 'index']);
$router->post('/api/correspondances/{id}/attachments',                      [CorrespondanceAttachmentController::class, 'store']);
$router->put('/api/correspondances/{id}/attachments/{attachId}',            [CorrespondanceAttachmentController::class, 'update']);
$router->delete('/api/correspondances/{id}/attachments/{attachId}',         [CorrespondanceAttachmentController::class, 'destroy']);
$router->get('/api/correspondances/{id}/attachments/{attachId}/download',   [CorrespondanceAttachmentController::class, 'download']);

// ========================
// Déclaration de perte Routes
// ========================
$router->get('/api/declarations-perte',           [DeclarationPerteController::class, 'index']);
$router->get('/api/declarations-perte/{id}',      [DeclarationPerteController::class, 'show']);
$router->post('/api/declarations-perte',          [DeclarationPerteController::class, 'store']);
$router->put('/api/declarations-perte/{id}',      [DeclarationPerteController::class, 'update']);
$router->delete('/api/declarations-perte/{id}',   [DeclarationPerteController::class, 'destroy']);

// ========================
// Déclaration de perte Attachment Routes
// ========================
$router->get('/api/declarations-perte/{id}/attachments',                       [DeclarationPerteAttachmentController::class, 'index']);
$router->post('/api/declarations-perte/{id}/attachments',                      [DeclarationPerteAttachmentController::class, 'store']);
$router->put('/api/declarations-perte/{id}/attachments/{attachId}',            [DeclarationPerteAttachmentController::class, 'update']);
$router->delete('/api/declarations-perte/{id}/attachments/{attachId}',         [DeclarationPerteAttachmentController::class, 'destroy']);
$router->get('/api/declarations-perte/{id}/attachments/{attachId}/download',   [DeclarationPerteAttachmentController::class, 'download']);

// ========================
// Passation Routes (Sédentaire > Poste)
// ========================
$router->get('/api/passations',           [PassationController::class, 'index']);
$router->get('/api/passations/{id}',      [PassationController::class, 'show']);
$router->post('/api/passations',          [PassationController::class, 'store']);
$router->put('/api/passations/{id}',      [PassationController::class, 'update']);
$router->delete('/api/passations/{id}',   [PassationController::class, 'destroy']);

// ========================
// Passation Attachment Routes
// ========================
$router->get('/api/passations/{id}/attachments',                       [PassationAttachmentController::class, 'index']);
$router->post('/api/passations/{id}/attachments',                      [PassationAttachmentController::class, 'store']);
$router->put('/api/passations/{id}/attachments/{attachId}',            [PassationAttachmentController::class, 'update']);
$router->delete('/api/passations/{id}/attachments/{attachId}',         [PassationAttachmentController::class, 'destroy']);
$router->get('/api/passations/{id}/attachments/{attachId}/download',   [PassationAttachmentController::class, 'download']);

// ========================
// Armement Routes (Sédentaire > Poste)
// ========================
$router->get('/api/armements',                            [ArmementController::class, 'index']);
$router->get('/api/armements/{id}',                       [ArmementController::class, 'show']);
$router->post('/api/armements',                           [ArmementController::class, 'store']);
$router->put('/api/armements/{id}',                       [ArmementController::class, 'update']);
$router->post('/api/armements/{id}/reintegration',        [ArmementController::class, 'reintegrate']);
$router->delete('/api/armements/{id}',                    [ArmementController::class, 'destroy']);

// ========================
// Armement Attachment Routes
// ========================
$router->get('/api/armements/{id}/attachments',                       [ArmementAttachmentController::class, 'index']);
$router->post('/api/armements/{id}/attachments',                      [ArmementAttachmentController::class, 'store']);
$router->put('/api/armements/{id}/attachments/{attachId}',            [ArmementAttachmentController::class, 'update']);
$router->delete('/api/armements/{id}/attachments/{attachId}',         [ArmementAttachmentController::class, 'destroy']);
$router->get('/api/armements/{id}/attachments/{attachId}/download',   [ArmementAttachmentController::class, 'download']);

// ========================
// Type d'arme Routes (catalogue — referenced by arme)
// ========================
$router->get('/api/types-armes',           [TypeArmeController::class, 'index']);
$router->get('/api/types-armes/{id}',      [TypeArmeController::class, 'show']);
$router->post('/api/types-armes',          [TypeArmeController::class, 'store']);
$router->put('/api/types-armes/{id}',      [TypeArmeController::class, 'update']);
$router->delete('/api/types-armes/{id}',   [TypeArmeController::class, 'destroy']);

// ========================
// Arme Routes (individual weapon instances — ammunition stock source of truth)
// ========================
$router->get('/api/armes',                              [ArmeController::class, 'index']);
$router->get('/api/armes/{id}',                         [ArmeController::class, 'show']);
$router->post('/api/armes',                             [ArmeController::class, 'store']);
$router->put('/api/armes/{id}',                         [ArmeController::class, 'update']);
$router->delete('/api/armes/{id}',                      [ArmeController::class, 'destroy']);
$router->get('/api/armes/{id}/consommations',           [ArmeController::class, 'consommations']);
$router->post('/api/armes/{id}/consommation',           [ArmeController::class, 'consommationAction']);

// ========================
// Type Matériel Routes (equipment type catalogue)
// ========================
$router->get('/api/types-materiels',           [TypeMaterielController::class, 'index']);
$router->get('/api/types-materiels/{id}',      [TypeMaterielController::class, 'show']);
$router->post('/api/types-materiels',          [TypeMaterielController::class, 'store']);
$router->put('/api/types-materiels/{id}',      [TypeMaterielController::class, 'update']);
$router->delete('/api/types-materiels/{id}',   [TypeMaterielController::class, 'destroy']);

// ========================
// Affectation Matériel Routes (equipment assignment & return)
// ========================
$router->get('/api/affectations-materiels',                    [AffectationMaterielController::class, 'index']);
$router->get('/api/affectations-materiels/{id}',               [AffectationMaterielController::class, 'show']);
$router->post('/api/affectations-materiels',                   [AffectationMaterielController::class, 'store']);
$router->put('/api/affectations-materiels/{id}',               [AffectationMaterielController::class, 'update']);
$router->post('/api/affectations-materiels/{id}/reintegration', [AffectationMaterielController::class, 'reintegrate']);
$router->delete('/api/affectations-materiels/{id}',            [AffectationMaterielController::class, 'destroy']);

// ========================
// Matériel Roulant Routes (vehicle perception & reintegration — VHL / Moto)
// ========================
$router->get('/api/materiels-roulants',                     [MaterielRoulantController::class, 'index']);
$router->get('/api/materiels-roulants/{id}',                [MaterielRoulantController::class, 'show']);
$router->post('/api/materiels-roulants',                    [MaterielRoulantController::class, 'store']);
$router->put('/api/materiels-roulants/{id}',                [MaterielRoulantController::class, 'update']);
$router->post('/api/materiels-roulants/{id}/reintegration', [MaterielRoulantController::class, 'reintegrate']);
$router->delete('/api/materiels-roulants/{id}',             [MaterielRoulantController::class, 'destroy']);
$router->get('/api/materiels-roulants/{id}/attachments',                       [MaterielRoulantAttachmentController::class, 'index']);
$router->post('/api/materiels-roulants/{id}/attachments',                      [MaterielRoulantAttachmentController::class, 'store']);
$router->put('/api/materiels-roulants/{id}/attachments/{attachId}',            [MaterielRoulantAttachmentController::class, 'update']);
$router->delete('/api/materiels-roulants/{id}/attachments/{attachId}',          [MaterielRoulantAttachmentController::class, 'destroy']);
$router->get('/api/materiels-roulants/{id}/attachments/{attachId}/download',   [MaterielRoulantAttachmentController::class, 'download']);

// ========================
// Main courante Routes (Sédentaire > Secrétariat & Poste)
// ========================
$router->get('/api/main-courante',           [MainCouranteController::class, 'index']);
$router->get('/api/main-courante/{id}',      [MainCouranteController::class, 'show']);
$router->post('/api/main-courante',          [MainCouranteController::class, 'store']);
$router->put('/api/main-courante/{id}',      [MainCouranteController::class, 'update']);
$router->delete('/api/main-courante/{id}',   [MainCouranteController::class, 'destroy']);

// ========================
// Main courante Attachment Routes
// ========================
$router->get('/api/main-courante/{id}/attachments',                       [MainCouranteAttachmentController::class, 'index']);
$router->post('/api/main-courante/{id}/attachments',                      [MainCouranteAttachmentController::class, 'store']);
$router->put('/api/main-courante/{id}/attachments/{attachId}',            [MainCouranteAttachmentController::class, 'update']);
$router->delete('/api/main-courante/{id}/attachments/{attachId}',         [MainCouranteAttachmentController::class, 'destroy']);
$router->get('/api/main-courante/{id}/attachments/{attachId}/download',   [MainCouranteAttachmentController::class, 'download']);

// ========================
// Main courante Categories (label catalog managed by users)
// ========================
$router->get('/api/main-courante-categories',          [MainCouranteCategorieController::class, 'index']);
$router->post('/api/main-courante-categories',         [MainCouranteCategorieController::class, 'store']);
$router->put('/api/main-courante-categories/{id}',    [MainCouranteCategorieController::class, 'update']);
$router->delete('/api/main-courante-categories/{id}', [MainCouranteCategorieController::class, 'destroy']);

// ========================
// Plainte ENTRÉE (Police Judiciaire — incoming complaints)
// ========================
$router->get('/api/plaintes-entree',                          [PlainteEntreeController::class, 'index']);
$router->get('/api/plaintes-entree/without-sortie',           [PlainteEntreeController::class, 'withoutSortie']);
$router->get('/api/plaintes-entree/next-number',              [PlainteEntreeController::class, 'nextNumber']);
$router->get('/api/plaintes-entree/{id}',                     [PlainteEntreeController::class, 'show']);
$router->post('/api/plaintes-entree',                         [PlainteEntreeController::class, 'store']);
$router->put('/api/plaintes-entree/{id}',                     [PlainteEntreeController::class, 'update']);
$router->delete('/api/plaintes-entree/{id}',                  [PlainteEntreeController::class, 'destroy']);

$router->get('/api/plaintes-entree/{id}/attachments',                       [PlainteEntreeAttachmentController::class, 'index']);
$router->post('/api/plaintes-entree/{id}/attachments',                      [PlainteEntreeAttachmentController::class, 'store']);
$router->put('/api/plaintes-entree/{id}/attachments/{attachId}',            [PlainteEntreeAttachmentController::class, 'update']);
$router->delete('/api/plaintes-entree/{id}/attachments/{attachId}',         [PlainteEntreeAttachmentController::class, 'destroy']);
$router->get('/api/plaintes-entree/{id}/attachments/{attachId}/download',   [PlainteEntreeAttachmentController::class, 'download']);

// ========================
// Plainte SORTIE (Police Judiciaire — outgoing processing of an ENTRÉE)
// ========================
$router->get('/api/plaintes-sortie',                          [PlainteSortieController::class, 'index']);
$router->get('/api/plaintes-sortie/next-number',              [PlainteSortieController::class, 'nextNumber']);
$router->get('/api/plaintes-sortie/{id}',                     [PlainteSortieController::class, 'show']);
$router->post('/api/plaintes-sortie',                         [PlainteSortieController::class, 'store']);
$router->put('/api/plaintes-sortie/{id}',                     [PlainteSortieController::class, 'update']);
$router->delete('/api/plaintes-sortie/{id}',                  [PlainteSortieController::class, 'destroy']);

$router->get('/api/plaintes-sortie/{id}/attachments',                       [PlainteSortieAttachmentController::class, 'index']);
$router->post('/api/plaintes-sortie/{id}/attachments',                      [PlainteSortieAttachmentController::class, 'store']);
$router->put('/api/plaintes-sortie/{id}/attachments/{attachId}',            [PlainteSortieAttachmentController::class, 'update']);
$router->delete('/api/plaintes-sortie/{id}/attachments/{attachId}',         [PlainteSortieAttachmentController::class, 'destroy']);
$router->get('/api/plaintes-sortie/{id}/attachments/{attachId}/download',   [PlainteSortieAttachmentController::class, 'download']);

// ========================
// Convocation Routes (Police Judiciaire — Convocation)
// ========================
$router->get('/api/convocations',                          [ConvocationController::class, 'index']);
$router->get('/api/convocations/next-number',              [ConvocationController::class, 'nextNumber']);
$router->get('/api/convocations/{id}',                     [ConvocationController::class, 'show']);
$router->post('/api/convocations',                         [ConvocationController::class, 'store']);
$router->put('/api/convocations/{id}',                     [ConvocationController::class, 'update']);
$router->delete('/api/convocations/{id}',                  [ConvocationController::class, 'destroy']);

$router->get('/api/convocations/{id}/attachments',                       [ConvocationAttachmentController::class, 'index']);
$router->post('/api/convocations/{id}/attachments',                      [ConvocationAttachmentController::class, 'store']);
$router->put('/api/convocations/{id}/attachments/{attachId}',            [ConvocationAttachmentController::class, 'update']);
$router->delete('/api/convocations/{id}/attachments/{attachId}',         [ConvocationAttachmentController::class, 'destroy']);
$router->get('/api/convocations/{id}/attachments/{attachId}/download',   [ConvocationAttachmentController::class, 'download']);

// ========================
// Garde à Vue Routes (Police Judiciaire — GAV)
// ========================
$router->get('/api/garde-a-vue',                          [GardeAVueController::class, 'index']);
$router->get('/api/garde-a-vue/{id}',                     [GardeAVueController::class, 'show']);
$router->post('/api/garde-a-vue',                         [GardeAVueController::class, 'store']);
$router->put('/api/garde-a-vue/{id}',                     [GardeAVueController::class, 'update']);
$router->delete('/api/garde-a-vue/{id}',                  [GardeAVueController::class, 'destroy']);

$router->get('/api/garde-a-vue/{id}/attachments',                       [GardeAVueAttachmentController::class, 'index']);
$router->post('/api/garde-a-vue/{id}/attachments',                      [GardeAVueAttachmentController::class, 'store']);
$router->put('/api/garde-a-vue/{id}/attachments/{attachId}',            [GardeAVueAttachmentController::class, 'update']);
$router->delete('/api/garde-a-vue/{id}/attachments/{attachId}',         [GardeAVueAttachmentController::class, 'destroy']);
$router->get('/api/garde-a-vue/{id}/attachments/{attachId}/download',   [GardeAVueAttachmentController::class, 'download']);

// ========================
// Requisition Routes (Police Judiciaire)
// ========================
$router->get('/api/requisitions',                          [RequisitionController::class, 'index']);
$router->get('/api/requisitions/next-number',               [RequisitionController::class, 'nextNumber']);
$router->get('/api/requisitions/{id}',                     [RequisitionController::class, 'show']);
$router->post('/api/requisitions',                         [RequisitionController::class, 'store']);
$router->put('/api/requisitions/{id}',                     [RequisitionController::class, 'update']);
$router->delete('/api/requisitions/{id}',                  [RequisitionController::class, 'destroy']);

$router->get('/api/requisitions/{id}/attachments',                       [RequisitionAttachmentController::class, 'index']);
$router->post('/api/requisitions/{id}/attachments',                      [RequisitionAttachmentController::class, 'store']);
$router->put('/api/requisitions/{id}/attachments/{attachId}',            [RequisitionAttachmentController::class, 'update']);
$router->delete('/api/requisitions/{id}/attachments/{attachId}',         [RequisitionAttachmentController::class, 'destroy']);
$router->get('/api/requisitions/{id}/attachments/{attachId}/download',   [RequisitionAttachmentController::class, 'download']);

// ========================
// Personne Recherchée Routes (Police Judiciaire)
// ========================
$router->get('/api/personne-recherchee',                                   [PersonneRechercheeController::class, 'index']);
$router->get('/api/personne-recherchee/{id}',                              [PersonneRechercheeController::class, 'show']);
$router->post('/api/personne-recherchee',                                  [PersonneRechercheeController::class, 'store']);
$router->put('/api/personne-recherchee/{id}',                              [PersonneRechercheeController::class, 'update']);
$router->delete('/api/personne-recherchee/{id}',                           [PersonneRechercheeController::class, 'destroy']);

// Dedicated multi-image endpoints (NOT the generic attachment system)
$router->get('/api/personne-recherchee/{id}/photos',                       [PersonneRechercheeController::class, 'photosIndex']);
$router->post('/api/personne-recherchee/{id}/photos',                      [PersonneRechercheeController::class, 'photosStore']);
$router->put('/api/personne-recherchee/{id}/photos/{photoId}',             [PersonneRechercheeController::class, 'photosUpdate']);
$router->delete('/api/personne-recherchee/{id}/photos/{photoId}',          [PersonneRechercheeController::class, 'photosDestroy']);
$router->get('/api/personne-recherchee/{id}/photos/{photoId}/download',    [PersonneRechercheeController::class, 'photosDownload']);

// ========================
// Objet Routes (Police Judiciaire — OBJET SAISI / OBJET TROUVÉ)
// ========================
// Objet Saisi
$router->get('/api/objets/saisi',                                            [ObjetSaisiController::class, 'index']);
$router->get('/api/objets/saisi/{id}',                                       [ObjetSaisiController::class, 'show']);
$router->post('/api/objets/saisi',                                           [ObjetSaisiController::class, 'store']);
$router->put('/api/objets/saisi/{id}',                                       [ObjetSaisiController::class, 'update']);
$router->delete('/api/objets/saisi/{id}',                                    [ObjetSaisiController::class, 'destroy']);

$router->get('/api/objets/saisi/{id}/attachments',                           [ObjetSaisiAttachmentController::class, 'index']);
$router->post('/api/objets/saisi/{id}/attachments',                          [ObjetSaisiAttachmentController::class, 'store']);
$router->put('/api/objets/saisi/{id}/attachments/{attachId}',                [ObjetSaisiAttachmentController::class, 'update']);
$router->delete('/api/objets/saisi/{id}/attachments/{attachId}',             [ObjetSaisiAttachmentController::class, 'destroy']);
$router->get('/api/objets/saisi/{id}/attachments/{attachId}/download',       [ObjetSaisiAttachmentController::class, 'download']);

// Objet Trouvé
$router->get('/api/objets/trouve',                                           [ObjetTrouveController::class, 'index']);
$router->get('/api/objets/trouve/{id}',                                      [ObjetTrouveController::class, 'show']);
$router->post('/api/objets/trouve',                                          [ObjetTrouveController::class, 'store']);
$router->put('/api/objets/trouve/{id}',                                      [ObjetTrouveController::class, 'update']);
$router->delete('/api/objets/trouve/{id}',                                   [ObjetTrouveController::class, 'destroy']);

$router->get('/api/objets/trouve/{id}/attachments',                          [ObjetTrouveAttachmentController::class, 'index']);
$router->post('/api/objets/trouve/{id}/attachments',                         [ObjetTrouveAttachmentController::class, 'store']);
$router->put('/api/objets/trouve/{id}/attachments/{attachId}',               [ObjetTrouveAttachmentController::class, 'update']);
$router->delete('/api/objets/trouve/{id}/attachments/{attachId}',            [ObjetTrouveAttachmentController::class, 'destroy']);
$router->get('/api/objets/trouve/{id}/attachments/{attachId}/download',      [ObjetTrouveAttachmentController::class, 'download']);

// ========================
// Perquisition Routes (Police Judiciaire)
// ========================
$router->get('/api/perquisitions',                                           [PerquisitionController::class, 'index']);
$router->get('/api/perquisitions/next-number',                               [PerquisitionController::class, 'nextNumber']);
$router->get('/api/perquisitions/{id}',                                      [PerquisitionController::class, 'show']);
$router->post('/api/perquisitions',                                          [PerquisitionController::class, 'store']);
$router->put('/api/perquisitions/{id}',                                      [PerquisitionController::class, 'update']);
$router->delete('/api/perquisitions/{id}',                                   [PerquisitionController::class, 'destroy']);

$router->get('/api/perquisitions/{id}/attachments',                          [PerquisitionAttachmentController::class, 'index']);
$router->post('/api/perquisitions/{id}/attachments',                         [PerquisitionAttachmentController::class, 'store']);
$router->put('/api/perquisitions/{id}/attachments/{attachId}',               [PerquisitionAttachmentController::class, 'update']);
$router->delete('/api/perquisitions/{id}/attachments/{attachId}',            [PerquisitionAttachmentController::class, 'destroy']);
$router->get('/api/perquisitions/{id}/attachments/{attachId}/download',      [PerquisitionAttachmentController::class, 'download']);

// ========================
// Renseignement PJ Routes (Police Judiciaire)
// ========================
$router->get('/api/renseignements-pj',                                       [RenseignementPjController::class, 'index']);
$router->get('/api/renseignements-pj/{id}',                                  [RenseignementPjController::class, 'show']);
$router->post('/api/renseignements-pj',                                      [RenseignementPjController::class, 'store']);
$router->put('/api/renseignements-pj/{id}',                                  [RenseignementPjController::class, 'update']);
$router->delete('/api/renseignements-pj/{id}',                               [RenseignementPjController::class, 'destroy']);

$router->get('/api/renseignements-pj/{id}/attachments',                      [RenseignementPjAttachmentController::class, 'index']);
$router->post('/api/renseignements-pj/{id}/attachments',                     [RenseignementPjAttachmentController::class, 'store']);
$router->put('/api/renseignements-pj/{id}/attachments/{attachId}',           [RenseignementPjAttachmentController::class, 'update']);
$router->delete('/api/renseignements-pj/{id}/attachments/{attachId}',        [RenseignementPjAttachmentController::class, 'destroy']);
$router->get('/api/renseignements-pj/{id}/attachments/{attachId}/download',  [RenseignementPjAttachmentController::class, 'download']);

// ========================
// Mandat Routes (Police Judiciaire)
// ========================
$router->get('/api/mandats',                                                 [MandatController::class, 'index']);
$router->get('/api/mandats/next-number',                                     [MandatController::class, 'nextNumber']);
$router->get('/api/mandats/{id}',                                            [MandatController::class, 'show']);
$router->post('/api/mandats',                                                [MandatController::class, 'store']);
$router->put('/api/mandats/{id}',                                            [MandatController::class, 'update']);
$router->delete('/api/mandats/{id}',                                         [MandatController::class, 'destroy']);

$router->get('/api/mandats/{id}/attachments',                                [MandatAttachmentController::class, 'index']);
$router->post('/api/mandats/{id}/attachments',                               [MandatAttachmentController::class, 'store']);
$router->put('/api/mandats/{id}/attachments/{attachId}',                     [MandatAttachmentController::class, 'update']);
$router->delete('/api/mandats/{id}/attachments/{attachId}',                  [MandatAttachmentController::class, 'destroy']);
$router->get('/api/mandats/{id}/attachments/{attachId}/download',            [MandatAttachmentController::class, 'download']);

// ========================
// Arrestation Routes (Police Judiciaire)
// ========================
$router->get('/api/arrestations',                                                 [ArrestationController::class, 'index']);
$router->get('/api/arrestations/next-number',                                     [ArrestationController::class, 'nextNumber']);
$router->get('/api/arrestations/{id}',                                            [ArrestationController::class, 'show']);
$router->post('/api/arrestations',                                                [ArrestationController::class, 'store']);
$router->put('/api/arrestations/{id}',                                            [ArrestationController::class, 'update']);
$router->delete('/api/arrestations/{id}',                                         [ArrestationController::class, 'destroy']);

$router->get('/api/arrestations/{id}/attachments',                                [ArrestationAttachmentController::class, 'index']);
$router->post('/api/arrestations/{id}/attachments',                               [ArrestationAttachmentController::class, 'store']);
$router->put('/api/arrestations/{id}/attachments/{attachId}',                     [ArrestationAttachmentController::class, 'update']);
$router->delete('/api/arrestations/{id}/attachments/{attachId}',                  [ArrestationAttachmentController::class, 'destroy']);
$router->get('/api/arrestations/{id}/attachments/{attachId}/download',            [ArrestationAttachmentController::class, 'download']);

// ========================
// Rassemblement Journalier Routes (Service Général)
// ========================
$router->get('/api/rassemblements',                                                [RassemblementJournalierController::class, 'index']);
$router->get('/api/rassemblements/{id}',                                           [RassemblementJournalierController::class, 'show']);
$router->post('/api/rassemblements',                                               [RassemblementJournalierController::class, 'store']);
$router->put('/api/rassemblements/{id}',                                            [RassemblementJournalierController::class, 'update']);
$router->delete('/api/rassemblements/{id}',                                         [RassemblementJournalierController::class, 'destroy']);

// ========================
// Role Routes (RBAC - SUPER_ADMIN only)
// ========================
$router->get('/api/roles',                   [RoleController::class, 'index']);
$router->get('/api/roles/{id}',              [RoleController::class, 'show']);
$router->post('/api/roles',                  [RoleController::class, 'store']);
$router->put('/api/roles/{id}',              [RoleController::class, 'update']);
$router->delete('/api/roles/{id}',           [RoleController::class, 'destroy']);
$router->get('/api/roles/{id}/permissions',  [RoleController::class, 'permissions']);
$router->put('/api/roles/{id}/permissions',  [RoleController::class, 'updatePermissions']);

// ========================
// User Routes
// ========================
$router->get('/api/users',          [UserController::class, 'index']);
$router->get('/api/users/{id}',     [UserController::class, 'show']);
$router->post('/api/users',         [UserController::class, 'store']);
$router->put('/api/users/{id}',     [UserController::class, 'update']);
$router->delete('/api/users/{id}',  [UserController::class, 'destroy']);

// ========================
// Notification Routes
// ========================
$router->get('/api/notifications',                [NotificationController::class, 'index']);
$router->get('/api/notifications/unread-count',    [NotificationController::class, 'unreadCount']);
$router->get('/api/notifications/{id}',            [NotificationController::class, 'show']);
$router->post('/api/notifications',                [NotificationController::class, 'store']);
$router->put('/api/notifications/{id}/read',       [NotificationController::class, 'markAsRead']);
$router->put('/api/notifications/read-all',        [NotificationController::class, 'markAllAsRead']);
$router->delete('/api/notifications/{id}',         [NotificationController::class, 'destroy']);

// ========================
// Device Token Routes (FCM Push Notifications)
// ========================
$router->get('/api/devices',               [DeviceTokenController::class, 'index']);
$router->post('/api/devices/register',     [DeviceTokenController::class, 'register']);
$router->post('/api/devices/unregister',   [DeviceTokenController::class, 'unregister']);
$router->delete('/api/devices',            [DeviceTokenController::class, 'unregisterAll']);
$router->post('/api/devices/test-push',    [DeviceTokenController::class, 'testPush']);

// ========================
// Audit Log Routes (SUPER_ADMIN only)
// ========================
$router->get('/api/audit-logs',       [AuditLogController::class, 'index']);
$router->get('/api/audit-logs/{id}',  [AuditLogController::class, 'show']);

// ========================
// Health Check
// ========================
$router->get('/api/health', function () {
    echo json_encode([
        'success' => true,
        'message' => 'OPUS API is running',
        'version' => '1.0.0',
        'time'    => date('c'),
    ]);
});

// --- Dispatch ---
$method = $_SERVER['REQUEST_METHOD'];
$uri    = $_SERVER['REQUEST_URI'];

$router->dispatch($method, $uri);
