package com.lakshay.arxplorer.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    val pages = listOf(
        OnboardingPage(
            title = "Discover Research Papers",
            description = "Find the latest research papers based on your interests using the arXiv database",
            imageRes = R.drawable.discover_papers
        ),
        OnboardingPage(
            title = "Read & Download",
            description = "Read papers online or download them for offline access. Bookmark your favorites for later",
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
            activeColor = MaterialTheme.colorScheme.primary
        )

        AnimatedVisibility(
            visible = pagerState.currentPage == pages.size - 1,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onSignInClick,
                modifier = Modifier
                    .padding(horizontal = 40.dp)
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
            ) {
                Text(
                    text = "Sign in with Google",
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
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
                    }
                ) {
                    Text(text = "Skip")
                }
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                ) {
                    Text(text = "Next")
                }
            }
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
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = onboardingPage.description,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
    }
}