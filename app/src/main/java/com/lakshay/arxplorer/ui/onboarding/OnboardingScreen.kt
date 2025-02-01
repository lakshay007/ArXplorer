package com.lakshay.arxplorer.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lakshay.arxplorer.R
import com.google.accompanist.pager.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalPagerApi::class)
@Composable
fun OnboardingScreen(
    onSignInClick: () -> Unit
) {
    val lightPurple = Color(0xFFF3E5F5)
    val mediumPurple = Color(0xFFE1BEE7)
    val deepPurple = Color(0xFF4A148C)
    
    val gradient = Brush.verticalGradient(
        colors = listOf(
            Color.White,
            lightPurple,
            mediumPurple.copy(alpha = 0.5f)
        )
    )

    val pages = listOf(
        OnboardingPage(
            title = "Discover Research Papers",
            description = "Find the latest research papers based on your interests using the arXiv database",
            imageRes = R.drawable.discover_papers
        ),
        OnboardingPage(
            title = "Read & Download",
            description = "Read papers online or download them for offline access and share them. Bookmark your favorites for later and get the summary and final results of papers in a single click.",
            imageRes = R.drawable.read_papers
        ),
        OnboardingPage(
            title = "AI-Powered Insights",
            description = "Chat with papers using AI to get summaries and better understand complex research",
            imageRes = R.drawable.ai_chat
        )
    )

    val pagerState = rememberPagerState()
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            HorizontalPager(
                count = pages.size,
                state = pagerState,
                modifier = Modifier
                    .weight(0.8f)
                    .fillMaxWidth()
            ) { position ->
                PagerScreen(onboardingPage = pages[position])
            }

            HorizontalPagerIndicator(
                pagerState = pagerState,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(16.dp),
                activeColor = deepPurple,
                inactiveColor = deepPurple.copy(alpha = 0.3f)
            )

            AnimatedVisibility(
                visible = pagerState.currentPage == pages.size - 1,
                modifier = Modifier.fillMaxWidth()
            ) {
                GoogleSignInButton(
                    onClick = onSignInClick,
                    modifier = Modifier
                        .padding(horizontal = 40.dp)
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                )
            }

            if (pagerState.currentPage != pages.size - 1) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 40.dp)
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pages.size - 1)
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = deepPurple
                        )
                    ) {
                        Text(text = "Skip")
                    }
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = deepPurple
                        )
                    ) {
                        Text(text = "Next")
                    }
                }
            }
        }
    }
}

@Composable
fun GoogleSignInButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedButton(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        colors = ButtonDefaults.elevatedButtonColors(
            containerColor = Color.White,
            contentColor = Color.Black
        ),
        shape = RoundedCornerShape(28.dp),
        elevation = ButtonDefaults.elevatedButtonElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.google_logo),
                contentDescription = "Google Logo",
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Sign in with Google",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
        }
    }
}

@Composable
fun PagerScreen(onboardingPage: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = onboardingPage.imageRes),
            contentDescription = "Onboarding Image",
            modifier = Modifier
                .size(200.dp)
                .fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = onboardingPage.title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = onboardingPage.description,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            color = Color.DarkGray
        )
    }
}