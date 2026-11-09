package com.ubayanda.milonative.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ubayanda.milonative.R
import com.ubayanda.milonative.ui.AccountUi
import com.ubayanda.milonative.ui.MainViewModel
import com.ubayanda.milonative.ui.components.LeavesBackground
import com.ubayanda.milonative.ui.theme.Milo
import com.ubayanda.milonative.util.TERMS_TEXT

/** Welcome, sign up, log in, pick a username, agree to terms: same flow and wording as the web milo. */
@Composable
fun GateScreen(vm: MainViewModel, a: AccountUi) {
    BackHandler(enabled = a.step == "up" || a.step == "in") { vm.gateStep("welcome") }
    Box(Modifier.fillMaxSize().background(Color(0xFF050505))) {
        if (a.step == "welcome") LeavesBackground(Modifier.fillMaxSize())
        when (a.step) {
            "welcome" -> Welcome(vm, a)
            "up", "in" -> Form(vm, a, signUp = a.step == "up")
            "claim" -> Claim(vm, a)
            "terms" -> Terms(vm, a)
        }
    }
}

@Composable
private fun PrimaryButton(label: String, busy: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(27.dp))
            .background(if (enabled && !busy) Milo.amber else Milo.amber.copy(alpha = 0.4f))
            .clickable(enabled = enabled && !busy, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(if (busy) "ONE MOMENT…" else label.uppercase(), color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
    }
}

@Composable
private fun OutlineButton(label: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(27.dp)
    Box(
        Modifier.fillMaxWidth().height(54.dp).clip(shape).background(Color(0xFF111111), shape).border(1.dp, Milo.line, shape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label.uppercase(), color = Milo.text, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
    }
}

@Composable
private fun Welcome(vm: MainViewModel, a: AccountUi) {
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 28.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(painterResource(R.drawable.logo_wordmark), null, Modifier.height(54.dp))
        Spacer(Modifier.height(22.dp))
        Text("Your music.\nYour people.", color = Milo.text, fontSize = 38.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, lineHeight = 42.sp)
        Spacer(Modifier.height(10.dp))
        Text("Playlists, friends and chat, on every device.", color = Milo.dim, fontSize = 16.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(26.dp))
        // Google sign-in errors used to be invisible here, which looked like "nothing happens"
        if (a.err.isNotEmpty()) { ErrorText(a.err); Spacer(Modifier.height(10.dp)) }
        PrimaryButton("Sign up free", false) { vm.gateStep("up") }
        Spacer(Modifier.height(12.dp))
        OutlineButton("Log in") { vm.gateStep("in") }
        if (com.ubayanda.milonative.data.GoogleSignIn.available) {
            Spacer(Modifier.height(12.dp))
            val ctx = androidx.compose.ui.platform.LocalContext.current
            OutlineButton(if (a.busy) "Signing in…" else "Continue with Google") { if (!a.busy) vm.signInGoogle(ctx) }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Continue without an account", color = Milo.dim, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { vm.skipGate() }.padding(8.dp),
        )
        Text("By continuing you agree to the terms, shown on the next screens.", color = Milo.dim.copy(alpha = 0.7f), fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun field(value: String, onChange: (String) -> Unit, hint: String, kb: KeyboardType, secret: Boolean = false) {
    OutlinedTextField(
        value = value, onValueChange = onChange, singleLine = true,
        placeholder = { Text(hint, color = Milo.dim) },
        shape = RoundedCornerShape(20.dp),
        visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = kb),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Milo.card, unfocusedContainerColor = Milo.card,
            focusedBorderColor = Milo.amber, unfocusedBorderColor = Milo.line,
            cursorColor = Milo.amber, focusedTextColor = Milo.text, unfocusedTextColor = Milo.text,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun Shell(title: String, sub: String, onBack: (() -> Unit)?, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (onBack != null) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(22.dp)).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.ArrowBackIosNew, "Back", tint = Milo.text)
            }
        } else Spacer(Modifier.height(44.dp))
        Image(painterResource(R.drawable.logo_wordmark), null, Modifier.height(40.dp))
        Text(title, color = Milo.text, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
        Text(sub, color = Milo.dim, fontSize = 15.sp)
        content()
    }
}

@Composable
private fun ErrorText(msg: String) {
    if (msg.isNotEmpty()) Text(msg, color = Color(0xFFFF6B5E), fontSize = 14.sp)
}

@Composable
private fun Form(vm: MainViewModel, a: AccountUi, signUp: Boolean) {
    var email by rememberSaveable { mutableStateOf(a.email) }
    var user by rememberSaveable { mutableStateOf("") }
    var pass by rememberSaveable { mutableStateOf("") }
    Shell(
        if (signUp) "Create your account" else "Welcome back",
        if (signUp) "One account for playlists, friends and chat." else "Log in to pick up where you left off.",
        onBack = { vm.gateStep("welcome") },
    ) {
        field(email, { email = it }, "Email", KeyboardType.Email)
        if (signUp) field(user, { user = it }, "Username (letters, numbers, _)", KeyboardType.Ascii)
        field(pass, { pass = it }, "Password (6+ characters)", KeyboardType.Password, secret = true)
        ErrorText(a.err)
        if (!signUp) Text(
            "Forgot password?", color = Milo.amber, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable { vm.resetPassword(email) }.padding(vertical = 4.dp),
        )
        PrimaryButton(if (signUp) "Sign up" else "Log in", a.busy) { if (signUp) vm.signUp(email, user, pass) else vm.signIn(email, pass) }
        Text(
            if (signUp) "Already have an account? Log in" else "New here? Sign up free",
            color = Milo.amber, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().clickable { vm.gateStep(if (signUp) "in" else "up") }.padding(12.dp),
        )
    }
}

@Composable
private fun Claim(vm: MainViewModel, a: AccountUi) {
    var user by rememberSaveable { mutableStateOf("") }
    Shell("Pick a username", "This is how friends find you. 3 to 20 letters, numbers or _.", onBack = null) {
        field(user, { user = it }, "Username", KeyboardType.Ascii)
        ErrorText(a.err)
        PrimaryButton("Continue", a.busy) { vm.claimUsername(user) }
    }
}

@Composable
private fun Terms(vm: MainViewModel, a: AccountUi) {
    var ok by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Terms and how your information is used", color = Milo.text, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Milo.card).border(1.dp, Milo.line, RoundedCornerShape(18.dp))) {
            Text(TERMS_TEXT, color = Milo.dim, fontSize = 14.sp, lineHeight = 21.sp, modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp))
        }
        Row(Modifier.fillMaxWidth().clickable { ok = !ok }, verticalAlignment = Alignment.CenterVertically) {
            Checkbox(ok, { ok = it }, colors = CheckboxDefaults.colors(checkedColor = Milo.amber, checkmarkColor = Color.Black, uncheckedColor = Milo.dim))
            Text("I have read and agree to the terms, including how my synced playlists, shared listening and messages are used.", color = Milo.text, fontSize = 13.sp)
        }
        ErrorText(a.err)
        PrimaryButton("Agree and continue", a.busy, enabled = ok) { vm.agreeTerms() }
        OutlineButton("Sign out") { vm.signOut() }
    }
}
