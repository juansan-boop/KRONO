package com.krono.feature.profile.edit

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.krono.core.ui.icons.KronoIcons
import com.krono.core.ui.theme.DeepMidnight
import com.krono.core.ui.theme.ErrorColor
import com.krono.core.ui.theme.KronoGlass
import com.krono.core.ui.theme.KronoSizes
import com.krono.core.ui.theme.KronoSpacing
import com.krono.core.ui.theme.KronoTheme
import com.krono.core.ui.theme.OnPrimary
import com.krono.core.ui.theme.OnSurface
import com.krono.core.ui.theme.OnSurfaceVariant
import com.krono.core.ui.theme.Outline
import com.krono.core.ui.theme.OutlineVariant
import com.krono.core.ui.theme.PrimaryFixedDim
import com.krono.core.ui.theme.PrimaryMagenta
import com.krono.core.ui.theme.SecondaryCyan
import com.krono.core.ui.theme.SurfaceContainer
import com.krono.core.ui.theme.SurfaceContainerHigh
import com.krono.core.ui.theme.SurfaceContainerLow
import com.krono.core.ui.theme.glassSurface
import com.krono.feature.profile.R
import com.krono.feature.profile.ui.ProfileSaveError
import com.krono.feature.profile.ui.rememberAsyncAvatarBitmap

/**
 * Punto de entrada stateful para la pantalla de Edición de Perfil (F-19 CA-2).
 *
 * Conecta [EditProfileViewModel] con el Composable stateless [EditProfileScreen] y
 * escucha los eventos de un solo uso [EditProfileEvent].
 *
 * @param onBackClick Acción para navegar atrás descartando cambios.
 * @param onProfileSaved Acción invocada al persistir los cambios exitosamente.
 * @param viewModel Instancia del ViewModel inyectada mediante Hilt.
 */
@Composable
fun EditProfileRoute(
    onBackClick: () -> Unit,
    onProfileSaved: () -> Unit = onBackClick,
    viewModel: EditProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            viewModel.onAvatarSelected(uri.toString())
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                EditProfileEvent.ProfileSaved -> {
                    Toast.makeText(
                        context,
                        context.getString(R.string.edit_profile_success_toast),
                        Toast.LENGTH_SHORT,
                    ).show()
                    onProfileSaved()
                }
                EditProfileEvent.NavigateBack -> onBackClick()
            }
        }
    }

    EditProfileScreen(
        state = state,
        onBackClick = onBackClick,
        onNameChange = viewModel::onNameChange,
        onRoleChange = viewModel::onRoleChange,
        onAvatarClick = {
            imagePickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        },
        onSaveClick = viewModel::onSave,
        onCancelClick = viewModel::onCancel,
    )
}

/**
 * Pantalla stateless de Edición de Perfil de Usuario (F-19 CA-2).
 *
 * Diseñada bajo el Lumina Glass System utilizando exclusivamente tokens de `:core:core-ui`.
 * Contiene:
 * 1. Barra superior con navegación de regreso accesible.
 * 2. Editor visual de avatar con halo degradado y botón de cámara táctil.
 * 3. Formulario con campo editable de Nombre (con validación de errores), selector
 *    de Rol Académico/Profesional y campo informativo de Correo Electrónico.
 * 4. Botón primario de guardar con estado de carga interactivo y protección contra doble clic.
 * 5. Botón secundario de cancelar.
 *
 * @param state Estado inmutable de la pantalla ([EditProfileUiState]).
 * @param onBackClick Acción al presionar el botón de retorno.
 * @param onNameChange Callback al modificar el texto del nombre.
 * @param onRoleChange Callback al seleccionar un nuevo rol.
 * @param onAvatarClick Callback para abrir el selector de imagen de galería.
 * @param onSaveClick Acción para validar y persistir los cambios.
 * @param onCancelClick Acción para cancelar y descartar modificaciones.
 * @param modifier Modificador de Compose para personalización del contenedor.
 */
@Composable
fun EditProfileScreen(
    state: EditProfileUiState,
    onBackClick: () -> Unit,
    onNameChange: (String) -> Unit,
    onRoleChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    onCancelClick: () -> Unit,
    onAvatarClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepMidnight),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = KronoSizes.formMaxWidth)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = KronoSpacing.lg, vertical = KronoSpacing.md),
            verticalArrangement = Arrangement.spacedBy(KronoSpacing.lg),
        ) {
            // 1. Barra superior
            EditProfileTopBar(onBackClick = onBackClick)

            // 2. Sección del avatar interactivo
            EditProfileAvatarSection(
                initials = state.userInitials,
                avatarUri = state.avatarUri,
                onAvatarClick = onAvatarClick,
            )

            // 3. Formulario en superficie de vidrio Lumina Glass
            EditProfileForm(
                state = state,
                onNameChange = onNameChange,
                onRoleChange = onRoleChange,
            )

            // Mensaje de error general si ocurre fallo
            AnimatedVisibility(
                visible = state.saveError != null,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                state.saveError?.let { saveError ->
                    Surface(
                        shape = RoundedCornerShape(KronoSpacing.sm),
                        color = ErrorColor.copy(alpha = 0.15f),
                        border = BorderStroke(KronoGlass.borderWidth, ErrorColor.copy(alpha = 0.40f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(KronoSpacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(KronoSpacing.sm),
                        ) {
                            Icon(
                                painter = painterResource(KronoIcons.Error),
                                contentDescription = null,
                                tint = ErrorColor,
                                modifier = Modifier.size(KronoSizes.iconMedium),
                            )
                            Text(
                                text = stringResource(saveError.messageRes()),
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurface,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(KronoSpacing.xs))

            // 4. Acciones de pie de página: Guardar y Cancelar
            EditProfileFooterActions(
                isSaving = state.isSaving,
                onSaveClick = onSaveClick,
                onCancelClick = onCancelClick,
            )
        }
    }
}

/**
 * Barra superior de navegación para la edición de perfil.
 */
@Composable
private fun EditProfileTopBar(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = KronoSizes.controlHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KronoSpacing.xs),
    ) {
        IconButton(
            onClick = onBackClick,
            modifier = Modifier.size(KronoSizes.minTouchTarget),
        ) {
            Icon(
                painter = painterResource(KronoIcons.ArrowBack),
                contentDescription = stringResource(R.string.edit_profile_cd_back),
                tint = OnSurfaceVariant,
                modifier = Modifier.size(KronoSizes.iconMedium),
            )
        }
        Text(
            text = stringResource(R.string.edit_profile_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = OnSurface,
        )
    }
}

/**
 * Componente visual de avatar de usuario con halo degradado y botón de cámara interactivo.
 */
@Composable
private fun EditProfileAvatarSection(
    initials: String,
    avatarUri: String? = null,
    onAvatarClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(KronoSpacing.sm),
    ) {
        Box(
            modifier = Modifier.size(KronoSizes.brandLogo + KronoSpacing.sm),
            contentAlignment = Alignment.Center,
        ) {
            // Contenedor principal con halo degradado
            Box(
                modifier = Modifier
                    .size(KronoSizes.brandLogo)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(PrimaryMagenta, SecondaryCyan),
                        ),
                    )
                    .padding(KronoGlass.borderWidth * 2)
                    .clip(CircleShape)
                    .background(SurfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                val bitmap = rememberAsyncAvatarBitmap(avatarUri)
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = stringResource(R.string.edit_profile_cd_avatar),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryFixedDim,
                    )
                }
            }

            // Botón interactivo de cámara posicionado abajo a la derecha
            Surface(
                onClick = onAvatarClick,
                shape = CircleShape,
                color = PrimaryMagenta,
                border = BorderStroke(KronoGlass.borderWidth * 2, DeepMidnight),
                modifier = Modifier
                    .size(KronoSizes.minTouchTarget)
                    .align(Alignment.BottomEnd),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.edit_profile_cd_change_photo),
                        tint = OnPrimary,
                        modifier = Modifier.size(KronoSizes.iconSmall + KronoSpacing.xs / 2),
                    )
                }
            }
        }

        Text(
            text = stringResource(R.string.edit_profile_hint_change_photo),
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Formulario de datos de perfil en superficie Lumina Glass.
 */
@Composable
private fun EditProfileForm(
    state: EditProfileUiState,
    onNameChange: (String) -> Unit,
    onRoleChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .glassSurface(cornerRadius = KronoSpacing.lg)
            .padding(KronoSpacing.lg),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(KronoSpacing.md),
        ) {
            // Campo 1: Nombre Completo
            EditProfileNameField(
                name = state.name,
                nameError = if (state.isNameMissing) stringResource(R.string.edit_profile_error_name_empty) else null,
                onNameChange = onNameChange,
            )

            // Campo 2: Rol Académico / Profesional
            EditProfileRoleSelector(
                selectedRole = state.role,
                availableRoles = state.availableRoles,
                onRoleChange = onRoleChange,
            )

            // Campo 3: Correo Electrónico (Solo Lectura)
            EditProfileEmailReadOnlyField(
                email = state.email,
            )
        }
    }
}

/**
 * Campo de texto editable para el Nombre Completo con validación y estados de error.
 */
@Composable
private fun EditProfileNameField(
    name: String,
    nameError: String?,
    onNameChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(KronoSpacing.xs),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(KronoSpacing.xs),
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = PrimaryFixedDim,
                modifier = Modifier.size(KronoSizes.iconSmall),
            )
            Text(
                text = stringResource(R.string.edit_profile_label_name),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = OnSurfaceVariant,
            )
        }

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = KronoSizes.controlHeight),
            singleLine = true,
            isError = nameError != null,
            placeholder = {
                Text(
                    text = stringResource(R.string.edit_profile_placeholder_name),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Outline,
                )
            },
            trailingIcon = {
                if (nameError != null) {
                    Icon(
                        painter = painterResource(KronoIcons.Error),
                        contentDescription = nameError,
                        tint = ErrorColor,
                        modifier = Modifier.size(KronoSizes.iconMedium),
                    )
                } else if (name.isNotBlank()) {
                    Icon(
                        painter = painterResource(KronoIcons.CheckCircle),
                        contentDescription = null,
                        tint = SecondaryCyan,
                        modifier = Modifier.size(KronoSizes.iconMedium),
                    )
                }
            },
            shape = RoundedCornerShape(KronoSpacing.md),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceContainer,
                unfocusedContainerColor = SurfaceContainerLow,
                disabledContainerColor = SurfaceContainerLow,
                errorContainerColor = SurfaceContainerLow,
                focusedBorderColor = PrimaryMagenta,
                unfocusedBorderColor = OutlineVariant,
                errorBorderColor = ErrorColor,
                focusedTextColor = OnSurface,
                unfocusedTextColor = OnSurface,
            ),
        )

        if (nameError != null) {
            Text(
                text = nameError,
                style = MaterialTheme.typography.bodySmall,
                color = ErrorColor,
                modifier = Modifier.padding(start = KronoSpacing.xs),
            )
        }
    }
}

/**
 * Selector desplegable del rol académico y profesional bajo Lumina Glass.
 */
@Composable
private fun EditProfileRoleSelector(
    selectedRole: String,
    availableRoles: List<String>,
    onRoleChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(KronoSpacing.xs),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(KronoSpacing.xs),
        ) {
            Icon(
                painter = painterResource(KronoIcons.Checklist),
                contentDescription = null,
                tint = PrimaryFixedDim,
                modifier = Modifier.size(KronoSizes.iconSmall),
            )
            Text(
                text = stringResource(R.string.edit_profile_label_role),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = OnSurfaceVariant,
            )
        }

        Box(modifier = Modifier.fillMaxWidth()) {
            Surface(
                onClick = { isExpanded = !isExpanded },
                shape = RoundedCornerShape(KronoSpacing.md),
                color = SurfaceContainerLow,
                border = BorderStroke(KronoGlass.borderWidth, OutlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = KronoSizes.controlHeight),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = KronoSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = selectedRole,
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = stringResource(R.string.edit_profile_cd_role_selector),
                        tint = OnSurfaceVariant,
                        modifier = Modifier.size(KronoSizes.iconMedium),
                    )
                }
            }

            DropdownMenu(
                expanded = isExpanded,
                onDismissRequest = { isExpanded = false },
                modifier = Modifier
                    .background(SurfaceContainerHigh)
                    .border(KronoGlass.borderWidth, OutlineVariant, RoundedCornerShape(KronoSpacing.sm)),
            ) {
                availableRoles.forEach { roleOption ->
                    val isSelected = roleOption == selectedRole
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = roleOption,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) SecondaryCyan else OnSurface,
                            )
                        },
                        trailingIcon = {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = SecondaryCyan,
                                    modifier = Modifier.size(KronoSizes.iconSmall),
                                )
                            }
                        },
                        onClick = {
                            onRoleChange(roleOption)
                            isExpanded = false
                        },
                        modifier = Modifier.heightIn(min = KronoSizes.minTouchTarget),
                    )
                }
            }
        }
    }
}

/**
 * Campo informativo de solo lectura para el correo electrónico del usuario.
 */
@Composable
private fun EditProfileEmailReadOnlyField(
    email: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(KronoSpacing.xs),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(KronoSpacing.xs),
        ) {
            Icon(
                imageVector = Icons.Default.Email,
                contentDescription = null,
                tint = PrimaryFixedDim,
                modifier = Modifier.size(KronoSizes.iconSmall),
            )
            Text(
                text = stringResource(R.string.edit_profile_label_email),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = OnSurfaceVariant,
            )
        }

        Surface(
            shape = RoundedCornerShape(KronoSpacing.md),
            color = SurfaceContainerLow.copy(alpha = 0.50f),
            border = BorderStroke(KronoGlass.borderWidth, OutlineVariant.copy(alpha = 0.30f)),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = KronoSizes.controlHeight),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = KronoSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = stringResource(R.string.edit_profile_cd_lock_email),
                    tint = Outline,
                    modifier = Modifier.size(KronoSizes.iconSmall + KronoSpacing.xs / 2),
                )
            }
        }

        Text(
            text = stringResource(R.string.edit_profile_email_readonly_note),
            style = MaterialTheme.typography.labelSmall,
            color = Outline,
            modifier = Modifier.padding(start = KronoSpacing.xs),
        )
    }
}

/**
 * Botones de acción principales: Guardar Cambios (con estado de progreso) y Cancelar.
 */
@Composable
private fun EditProfileFooterActions(
    isSaving: Boolean,
    onSaveClick: () -> Unit,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(KronoSpacing.sm),
    ) {
        // Botón Primario: Guardar Cambios
        Button(
            onClick = onSaveClick,
            enabled = !isSaving,
            shape = RoundedCornerShape(KronoSpacing.md),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryMagenta,
                contentColor = OnPrimary,
                disabledContainerColor = PrimaryMagenta.copy(alpha = 0.50f),
                disabledContentColor = OnPrimary.copy(alpha = 0.70f),
            ),
            contentPadding = PaddingValues(horizontal = KronoSpacing.lg),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = KronoSizes.controlHeight),
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    strokeWidth = KronoSizes.progressStroke,
                    color = OnPrimary,
                    modifier = Modifier.size(KronoSizes.iconMedium),
                )
                Spacer(Modifier.size(KronoSpacing.sm))
                Text(
                    text = stringResource(R.string.edit_profile_action_saving),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            } else {
                Icon(
                    painter = painterResource(KronoIcons.TaskAlt),
                    contentDescription = null,
                    tint = OnPrimary,
                    modifier = Modifier.size(KronoSizes.iconMedium),
                )
                Spacer(Modifier.size(KronoSpacing.sm))
                Text(
                    text = stringResource(R.string.edit_profile_action_save),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        // Botón Secundario: Cancelar
        TextButton(
            onClick = onCancelClick,
            enabled = !isSaving,
            shape = RoundedCornerShape(KronoSpacing.md),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = KronoSizes.minTouchTarget),
        ) {
            Text(
                text = stringResource(R.string.edit_profile_action_cancel),
                style = MaterialTheme.typography.labelLarge,
                color = OnSurfaceVariant,
            )
        }
    }
}

@Preview(name = "EditProfileScreen - Estado Normal", showBackground = true, backgroundColor = 0xFF121221)
@Composable
private fun EditProfileScreenNormalPreview() {
    KronoTheme {
        EditProfileScreen(
            state = EditProfileUiState(),
            onBackClick = {},
            onNameChange = {},
            onRoleChange = {},
            onSaveClick = {},
            onCancelClick = {},
        )
    }
}

@Preview(name = "EditProfileScreen - Guardando", showBackground = true, backgroundColor = 0xFF121221)
@Composable
private fun EditProfileScreenSavingPreview() {
    KronoTheme {
        EditProfileScreen(
            state = EditProfileUiState(isSaving = true),
            onBackClick = {},
            onNameChange = {},
            onRoleChange = {},
            onSaveClick = {},
            onCancelClick = {},
        )
    }
}

/** Recurso de copy para cada causa de fallo al guardar el perfil. */
@StringRes
private fun ProfileSaveError.messageRes(): Int = when (this) {
    ProfileSaveError.INVALID_DATA -> R.string.edit_profile_error_invalid
    ProfileSaveError.UNEXPECTED -> R.string.edit_profile_error_generic
}
