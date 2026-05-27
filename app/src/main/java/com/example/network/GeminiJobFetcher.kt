package com.example.network

import android.util.Log
import com.example.BuildConfig
import com.example.data.JobPosting
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object GeminiJobFetcher {
    private const val TAG = "GeminiJobFetcher"

    // Network model to match Gemini JSON parsed response
    data class ParsedJob(
        val title: String,
        val company: String,
        val description: String,
        val originalPlatform: String,
        val url: String,
        val salary: String,
        val location: String,
        val experience: String,
        val techStack: String,
        val postedDate: String
    )

    suspend fun fetchJobsFromGemini(query: String): List<JobPosting> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API key is not set or placeholder!")
            return@withContext getLocalSampleSeedJobs(query)
        }

        val prompt = """
            당신은 한국의 주요 채용 포털(사람인, 잡코리아, 원티드, 로켓펀치, 블라인드하이어 등)을 실시간으로 수집하고 중복 공고를 집계하는 스마트 채용 공고 수집기 엔진입니다.
            
            수집 키워드: "$query"에 맞는 한국의 실제 혹은 사실적인 IT/기술 기업 채용 공고 최소 8개 ~ 최대 12개를 한국어로 생성 및 수집하세요.
            
            [공고 수집 구성 가이드]
            1. 주요 IT 기업(네이버, 카카오, 라인, 쿠팡, 우아한형제들, 토스, 당근, 무신사, 야놀자, 아파트아이 등) 및 성장이 유망한 스타트업 공고를 고루 구성하세요.
            2. 여러 사이트(사람인, 잡코리아, 원티드, 로켓펀치, 블라인드하이어)에서 각각 긁어온 것처럼 보이도록 originalPlatform 속성을 다르게 지정해주세요.
            3. ★핵심 미션(중복 제거 검증): 수집한 공고 중 최소 1~2쌍은 채용 사이트 경쟁 수집으로 발생하는 '중복 공고' 형태로 만들어야 합니다.
               예를 들어 '우아한형제들'에서 'iOS 개발자 채용' 공고를 냈는데, 이것이 '사람인'과 '잡코리아'에 각각 등록되어 제목이 약간 다르게 긁힌 상황을 연출하십시오:
               - 공고 A: 회사명: "우아한형제들 (배달의민족)", 제목: "배달의민족 iOS 앱 신입/경력 개발자 영입", 플랫폼: "사람인"
               - 공고 B: 회사명: "(주)우아한형제들", 제목: "[우아한형제들] 배달의민족 iOS 앱 개발자 채용 (신입/경력)", 플랫폼: "잡코리아"
               이 두 채용 공고는 동일한 포지션이며 이외의 조건(상세설명, 연봉, 위치, 기술스택)이 일치하거나 극도로 유사해야 합니다.
               또 다른 1쌍의 중복 공고(예: 당근마켓/원티드와 로켓펀치)도 만들어주세요.
            
            반드시 아래 필드 구조를 지닌 JSON 배열 형식으로만 응답하세요. 여백, 마크다운 기호 없이 순수한 JSON 결과만 반환해 주세요.
            JSON 객체 필드명:
            - "title": 채용 포지션 및 공고 제목 (예: "안드로이드 앱 개발자 (신입/경력)")
            - "company": 기업명 (예: "우아한형제들", "당근")
            - "description": 주요 업무(Responsibilities) 및 자격 요건(Requirements), 우대 사항 등을 포함한 상세 업무 공고문 (마크다운 형식을 사용하여 포맷팅)
            - "originalPlatform": 수집된 원래 플랫폼명 (반드시 '사람인', '잡코리아', '원티드', '로켓펀치', '블라인드하이어' 중 하나여야 함)
            - "url": 채용공고 지원 링크 (실제 해당 플랫폼의 현실적인 임의 가상 URL 생성, 예: "https://www.saramin.co.kr/zf_user/jobs/view?rec_idx=123456")
            - "salary": 연봉 및 처우 (예: "회사내규에 따름", "4,500 ~ 6,000만원", "추후 협의")
            - "location": 회사 주소 혹은 주요 근무지 (예: "서울 송파구", "경기 성남시 분당구")
            - "experience": 신입/경력 요건 (예: "신입/경력 1~3년", "경력 3년 이상", "무관")
            - "techStack": 콤마(,)로 구분된 핵심 기술스택 태그 문자열 (예: "Kotlin, Android SDK, Jetpack Compose, Coroutines")
            - "postedDate": 공고 등록일 (형식: "YYYY-MM-DD", 최근인 2026-05-xx 구간)
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.4f
            )
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: return@withContext getLocalSampleSeedJobs(query)

            Log.d(TAG, "Received raw json from Gemini: $jsonText")

            val jobPostings = mutableListOf<JobPosting>()
            val jsonArray = JSONArray(jsonText)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                jobPostings.add(
                    JobPosting(
                        title = obj.optString("title", "채용 공고"),
                        company = obj.optString("company", "알 수 없음"),
                        description = obj.optString("description", "상세 요강이 없습니다."),
                        platforms = obj.optString("originalPlatform", "포털"),
                        originalPlatforms = obj.optString("originalPlatform", "포털"),
                        url = obj.optString("url", "#"),
                        salary = obj.optString("salary", "회사내규에 따름"),
                        location = obj.optString("location", "전국"),
                        experience = obj.optString("experience", "무관"),
                        techStack = obj.optString("techStack", ""),
                        postedDate = obj.optString("postedDate", "2026-05-27"),
                        deadline = obj.optString("deadline", "상시채용"),
                        isFavorite = false,
                        isRead = false,
                        isHidden = false
                    )
                )
            }
            return@withContext jobPostings
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching from Gemini, falling back to local simulation: ${e.message}", e)
            return@withContext getLocalSampleSeedJobs(query)
        }
    }

    /**
     * Fallback mock aggregator that seeds realistic job listings with mock duplicates.
     * This provides consistent offline capability and standard results if the Gemini Key is missing.
     */
    fun getLocalSampleSeedJobs(query: String = ""): List<JobPosting> {
        val q = query.lowercase().trim()
        val allJobs = listOf(
            // DUPLICATE GROUP 1: Woowahan Brothers iOS (Saramin vs JobKorea) -> Matches on: Company + Title (normalized) + Deadline + Location
            JobPosting(
                title = "배달의민족 iOS 앱 신입/경력 개발자 영입",
                company = "우아한형제들 (배달의민족)",
                description = "### 주요업무\n- 배달의민족 앱 개발 및 안정적인 서비스 유지보수\n- 아키텍처 개선 및 테스트 코드 작성\n\n### 자격요건\n- Swift 및 iOS SDK에 뛰어난 이해도\n- UI컴포넌트 커스텀 제작 및 오토레이아웃 이해\n- 경력 1년 이상 혹은 이에 준하는 주도적 경험\n\n### 우대사항\n- CI/CD 구축 경험\n- 대규모 트래픽 서비스 운영 실무경험",
                platforms = "사람인",
                originalPlatforms = "사람인",
                url = "https://www.saramin.co.kr/zf_user/jobs/view?rec_idx=95924",
                salary = "회사내규에 따름",
                location = "서울 송파구",
                experience = "신입/경력(1년 이상)",
                techStack = "Swift, iOS, UIKit, Combine, CoreData",
                postedDate = "2026-05-26",
                deadline = "2026-06-15"
            ),
            JobPosting(
                title = "[우아한형제들] 배달의민족 iOS 앱 개발자 채용 (신입/경력)",
                company = "(주)우아한형제들",
                description = "### 주요업무\n- 배달의민족 앱 개발 및 안정적인 서비스 유지보수\n- 아키텍처 개선 및 테스트 코드 작성\n\n### 자격요건\n- Swift 및 iOS SDK에 뛰어난 이해도\n- UI컴포넌트 커스텀 제작 및 오토레이아웃 이해\n- 경력 1년 이상 혹은 이에 준하는 주도적 경험\n\n### 우대사항\n- CI/CD 구축 경험\n- 대규모 트래픽 서비스 운영 실무경험",
                platforms = "잡코리아",
                originalPlatforms = "잡코리아",
                url = "https://www.jobkorea.co.kr/Recruit/GI_Read/104938",
                salary = "회사내규에 따름",
                location = "서울 송파구",
                experience = "신입/경력",
                techStack = "Swift, iOS, UIKit, Combine, CoreData",
                postedDate = "2026-05-26",
                deadline = "2026-06-15"
            ),

            // DUPLICATE GROUP 2: Toss Android (JobKorea vs Saramin)
            JobPosting(
                title = "토스(Toss) Android Developer 채용",
                company = "비바리퍼블리카",
                description = "### 주요업무\n- Toss 앱의 공통 플랫폼 및 금융 기능 신규 피처 개발\n- 컴포즈(Jetpack Compose) 이관 및 렌더링 성능 최적화\n\n### 자격요건\n- Kotlin 및 Android SDK 실무 보유력\n- MVVM/MVI 클린 아키텍처에 능숙하신 분\n- 훌륭한 협업 능력을 지니신 분\n\n### 우대사항\n- 대규모 결제 및 트랜잭션 서비스 라이브 경험\n- 에이전트 성능 검출 Tool 사용력",
                platforms = "잡코리아",
                originalPlatforms = "잡코리아",
                url = "https://www.jobkorea.co.kr/Recruit/GI_Read/209384",
                salary = "5,000 ~ 9,000만원",
                location = "서울 강남구 테헤란로",
                experience = "경력 (연차 무관)",
                techStack = "Kotlin, Android SDK, Jetpack Compose, Coroutines, Flow",
                postedDate = "2026-05-25",
                deadline = "2026-06-20"
            ),
            JobPosting(
                title = "[Toss] Android 개발자 채용 (경력무관)",
                company = "비바리퍼블리카 [토스]",
                description = "### 주요업무\n- Toss 앱의 공통 플랫폼 및 금융 기능 신규 피처 개발\n- 컴포즈(Jetpack Compose) 이관 및 렌더링 성능 최적화\n\n### 자격요건\n- Kotlin 및 Android SDK 실무 보유력\n- MVVM/MVI 클린 아키텍처에 능숙하신 분\n- 훌륭한 협업 능력을 지니신 분\n\n### 우대사항\n- 대규모 결제 및 트랜잭션 서비스 라이브 경험\n- 에이전트 성능 검출 Tool 사용력",
                platforms = "사람인",
                originalPlatforms = "사람인",
                url = "https://www.saramin.co.kr/zf_user/jobs/view?rec_idx=82938",
                salary = "회사내규에 따름",
                location = "서울 강남구 테헤란로",
                experience = "무관",
                techStack = "Kotlin, Android, Jetpack Compose, Coroutines",
                postedDate = "2026-05-25",
                deadline = "2026-06-20"
            ),

            // DUPLICATE GROUP 3: Daangn Frontend (Wanted vs Linkareer)
            JobPosting(
                title = "[당근마켓] 프론트엔드 엔지니어 (Frontend Engineer) - 당근페이",
                company = "당근 (Carrot)",
                description = "### 주요업무\n- 당근페이 관련 웹뷰 애플리케이션 및 어드민 웹툴 설계/구현\n- 모던 리액트 환경 및 공통 디자인 시스템 컴포넌트 고도화\n\n### 자격요건\n- React, Next.js, Node.js 기반 개발 역량\n- HTML5, CSS3, ES6+ 명세에 해박한 인플루언서\n- 정교한 UI 인터랙션 설계 능력\n\n### 우대사항\n- 금융/결제 도메인 실무 개발 역량 보유\n- 웹 성능 진단 및 번들 크기 경량화 성취가 있으신 분",
                platforms = "원티드",
                originalPlatforms = "원티드",
                url = "https://www.wanted.co.kr/wd/49204",
                salary = "회사내규에 따름",
                location = "서울 서초구",
                experience = "경력 3년 이상",
                techStack = "React, Next.js, TypeScript, TailWind, Webpack",
                postedDate = "2026-05-24",
                deadline = "상시채용"
            ),
            JobPosting(
                title = "프론트엔드 개발자 (당근페이 서비스)",
                company = "당근",
                description = "### 주요업무\n- 당근페이 관련 웹뷰 애플리케이션 및 어드민 웹툴 설계/구현\n- 모던 리액트 환경 및 공통 디자인 시스템 컴포넌트 고도화\n\n### 자격요건\n- React, Next.js, Node.js 기반 개발 역량\n- HTML5, CSS3, ES6+ 명세에 해박한 인플루언서\n- 정교한 UI 인터랙션 설계 능력\n\n### 우대사항\n- 금융/결제 도메인 실무 개발 역량 보유\n- 웹 성능 진단 및 번들 크기 경량화 성취가 있으신 분",
                platforms = "링커리어",
                originalPlatforms = "링커리어",
                url = "https://linkareer.com/activity/19223",
                salary = "회사내규에 따름",
                location = "서울 서초구",
                experience = "경력 3년 이상",
                techStack = "React, TypeScript, Next.js, TailwindCSS",
                postedDate = "2026-05-24",
                deadline = "상시채용"
            ),

            // OTHERS - SINGLE POSITIONS
            JobPosting(
                title = "플랫폼 백엔드(Platform Backend) 엔지니어 영입",
                company = "무신사",
                description = "### 주요업무\n- 대규모 커머스 시스템 주문/결제/정산 백엔드 컴포넌트 리팩토링\n- 도메인 중심 설계(DDD) 및 MSA 마이그레이션 아키텍팅\n\n### 자격요건\n- Java 17, Spring Boot, JPA 프로그래밍 능력 상위권 자율 수행자\n- RDBMS 대량 트랜잭션 튜닝 및 NoSQL 분산 구조 경험 필수\n- 경력 5년 이상\n\n### 우대사항\n- AWS, Kubernetes 클라우드 네이티브 설계 통섭자\n- 이벤트 드리븐 아키텍처(Kafka) 실무 튜닝 보유자",
                platforms = "인크루트",
                originalPlatforms = "인크루트",
                url = "https://www.incruit.com/jobs/829384",
                salary = "6,000 ~ 9,500만원",
                location = "서울 성동구",
                experience = "경력 5년 이상",
                techStack = "Java, Spring Boot, JPA, QueryDSL, MySQL, Redis, AWS",
                postedDate = "2026-05-27",
                deadline = "2026-06-10"
            ),
            JobPosting(
                title = "AI 검색 추천 서비스 PM 채용",
                company = "야놀자",
                description = "### 주요업무\n- 여행/숙박 검색 정밀 랭킹 및 머신러닝 추천 모델 개선 로드맵 수립\n- 데이터 지표 파악 및 기획안 가설 점검 AB테스트 진행\n\n### 자격요건\n- Tech PM/PO 경험 최소 3년 이상 보유\n- 지표 대시보드(SQL, Tableau) 기반 가설 수립 및 검증 자율 수행\n\n### 우대사항\n- AI 추천 알고리즘 협업 및 LLM 도입 기획 진행 성취\n- 글로벌 숙박/여행 어플리케이션 운영PM 기교 보유",
                platforms = "자소설닷컴",
                originalPlatforms = "자소설닷컴",
                url = "https://jasoseol.com/recruit/89234",
                salary = "회사내규에 따름",
                location = "서울 강남구",
                experience = "경력 3년 이상",
                techStack = "Product Management, Data Analysis, SQL, AB Test",
                postedDate = "2026-05-27",
                deadline = "2026-05-31"
            ),
            JobPosting(
                title = "[신입] 라이브 서비스 운영 어시스턴트 디자이너 채용",
                company = "카카오게임즈",
                description = "### 주요업무\n- 이벤트 배너, 마케팅 프로모션 배포용 그래픽 애셋 제작\n- SNS 포스트 소셜 채널용 썸네일 그래픽 작업\n\n### 자격요건\n- Photoshop/Illustrator 어도비 디자인 제품군 숙련자\n- 모바일게임/PC온라인게임 트렌드에 빠삭하신 분\n- 포트폴리오 첨부 필수\n\n### 우대사항\n- Figma 및 모던 UI 툴을 사용해 상호작용 가능한 목업을 만들 줄 아시는 분\n- 캐주얼/판타지 원화 드로잉 실질 취미자",
                platforms = "워크넷/고용24",
                originalPlatforms = "워크넷/고용24",
                url = "https://www.work.go.kr/empInfo/empInfoSrch/detail/empInfoDetail.do?wantedAuthNo=23948",
                salary = "3,200 ~ 3,800만원",
                location = "경기 성남시 판교",
                experience = "신입",
                techStack = "Photoshop, Illustrator, Figma, Graphic Design",
                postedDate = "2026-05-25",
                deadline = "2026-06-12"
            ),
            JobPosting(
                title = "클라우드 인프라 DevSecOps 엔지니어",
                company = "직방",
                description = "### 주요업무\n- AWS 가상 클라우드 네트워크 아키텍처 인프라 테라폼 배포 및 모니터링\n- 도커(Docker) 빌드 파이프라인 및 침입 탐지/보안 검사 자동화 가동\n\n### 자격요건\n- AWS 설계 역량 및 Linux 커널/네트워크 기본 이해\n- CI/CD 배포 엔진(GitHub Actions) 숙련\n\n### 우대사항\n- Kubernetes 및 Helm Chart 실무 운영 이력\n- 정보보안 관련 자격증 보유자",
                platforms = "캐치",
                originalPlatforms = "캐치",
                url = "https://www.catch.co.kr/Recruit/View/203948",
                salary = "4,500 ~ 7,000만원",
                location = "서울 서초구",
                experience = "경력 2년 이상",
                techStack = "AWS, Docker, Terraform, CI/CD, Kubernetes, Linux",
                postedDate = "2026-05-24",
                deadline = "2026-06-25"
            ),
            JobPosting(
                title = "데이터 분석가 (Data Analyst)",
                company = "버킷플레이스 (오늘의집)",
                description = "### 주요업무\n- 이커머스 장바구니 전환 지표 수립 및 퍼널 최적화 요인 수집\n- Amplitude 행동 데이터 모니터링 및 마케팅 성과 측정 통계 검정\n\n### 자격요건\n- SQL 데이터 정제 및 파이썬 데이터 핸들링(Pandas) 숙련\n- 논리적 커뮤니케이션 능력을 갖춘 분\n\n### 우대사항\n- 커머스 서비스 지표 개선 주도 이력이 있으신 분\n- 대용량 데이터 쿼리 최적화 경험",
                platforms = "잡플래닛",
                originalPlatforms = "잡플래닛",
                url = "https://www.jobplanet.co.kr/job/search/120938",
                salary = "회사내규에 따름",
                location = "서울 강남구",
                experience = "경력무관",
                techStack = "SQL, Python, Pandas, Tableau, Amplitude",
                postedDate = "2026-05-23",
                deadline = "상시채용"
            ),
            JobPosting(
                title = "HR 서비스 개발 기획자 (Product Owner)",
                company = "드라마앤컴퍼니 [리멤버]",
                description = "### 주요업무\n- 리멤버 커리어 비즈니스 B2B 채용 솔루션 제품 기능 기획\n- 구인/구직 매칭 알고리즘 정밀화 및 기업 유저 인박스 개선 수립\n\n### 자격요건\n- IT 서비스 PO/PM 또는 IT 기획 경력 4년 이상 보유\n- 유저 저니 분석 기반 프로덕트 상세 요건 설계서 작성 숙련\n\n### 우대사항\n- 채용 플랫폼 또는 검색 매칭 엔진 도메인 지식 풍부하신 분\n- 데이터 지표 파악 및 SQL 활용력",
                platforms = "리멤버 커리어",
                originalPlatforms = "리멤버 커리어",
                url = "https://career.rememberapp.co.kr/posts/304928",
                salary = "회사내규에 따름",
                location = "서울 강남구",
                experience = "경력 4년 이상",
                techStack = "Product Management, Product Owner, UX, Data Analysis",
                postedDate = "2026-05-22",
                deadline = "상시채용"
            )
        )

        val filtered = if (q.isEmpty()) {
            allJobs
        } else {
            allJobs.filter {
                it.title.lowercase().contains(q) ||
                it.company.lowercase().contains(q) ||
                it.techStack.lowercase().contains(q) ||
                it.location.lowercase().contains(q)
            }
        }

        // Apply fallback duplicates if search filtered out the original groups to make sure user still gets deduplication demo
        if (q.isNotEmpty() && filtered.size < 3) {
            return (filtered + allJobs.take(3)).distinctBy { it.title + it.originalPlatforms }
        }

        return filtered
    }
}
