# 📱 Android Scouter App

### CameraX + ML Kit Face Detection + Jetpack Compose Overlay 기반 전투력 측정 스카우터 앱

이 프로젝트는 카메라로 얼굴을 인식하여 드래곤볼 스카우터처럼 전투력을 측정해 시각화하는 앱이다.
얼굴 인식은 ML Kit을 활용하고, 전투력 측정 로직 및 상태 관리는 Clean Architecture 기반의 여러 모듈로 나누어 구현한다.

<table>
  <tbody>
    <tr><td>측정중</td><td>측정완료</td></tr>
    <tr>
      <td><img width="1000" height="600" alt="scouter1" src="https://github.com/user-attachments/assets/2fe60b14-f91d-4e40-98ae-9373618c44ac" /></td>
      <td><img width="1000" height="600" alt="scouter2" src="https://github.com/user-attachments/assets/7ca8d175-0e70-4c05-8e80-97d4e037f095" /></td>
    </tr>
    <tr><td colspan="2">다중 얼굴 인식</td></tr>
    <tr><td colspan="2"><img width="1000" height="600" alt="scouter3" src="https://github.com/user-attachments/assets/1b662346-a041-4a11-bb48-d2a3bcac0de0" /></td></tr>
  </tbody>
</table>

## ✨ 주요 기능

### 🔍 얼굴 인식 (ML Kit Face Detection)
- CameraX ImageAnalysis 를 통해 실시간 프레임을 받고
- ML Kit FaceDetection 으로 얼굴 boundingBox + 표정 확률 등을 분석
- trackingId 사용 → 동일 얼굴 식별
- **다중 얼굴 인식 지원: 화면에 있는 모든 얼굴을 개별적으로 인식 및 추적**

### 🔥 전투력 측정 알고리즘
- 얼굴 크기, 스마일 확률, 눈 떠있는 정도 등을 조합하여 전투력 계산
- 3초 동안 측정한 전투력의 평균값 표시
- **각 얼굴별 독립적인 측정 진행**

### 🎛 실시간 UI 오버레이 (Jetpack Compose Canvas)
- Measuring 상태 → 얼굴 위치에서 정사각형 회전 애니메이션
- Done 상태 → 얼굴 boundingBox 영역에 전투력 표시 박스 + 텍스트
- FILL_CENTER 기반 PreviewView scaling 적용
  → ML Kit boundingBox를 화면 좌표로 정확히 매핑
- **여러 얼굴에 대해 동시에 오버레이 표시**

### 🧠 상태 관리(PowerMeasurementStateMachine)
- Idle → Measuring → Done 전환 관리 (각 얼굴별 독립적)
- 얼굴 사라질 경우 해당 얼굴의 측정 상태 제거 (Measuring/Done 모두)
- 화면에 남아있는 얼굴은 계속 측정 진행

### 🧩 모듈 구조 (Clean Architecture)
```
:core:model      ← 순수 Kotlin 데이터 모델(FaceRect, FrameData, FaceMeasurementState 등)
:core:logic      ← 전투력 계산/상태 머신 등 비즈니스 로직
:core:ml         ← ML Kit 얼굴 분석 로직 (FaceDetector 인터페이스 구현)
:app:mobile      ← CameraX/Compose UI/Overlay/DI
```

## 🏗 아키텍처 구성
### 📦 모듈 구조
```markdown
project-root
 ├─ core/
 │   ├─ model/      (순수 Kotlin 데이터 클래스)
 │   ├─ logic/      (상태 머신 + 전투력 계산)
 │   └─ ml/         (ML Kit 기반 FaceDetector)
 └─ app/
     └─ mobile/     (CameraX + Compose UI)
```

## 🧱 핵심 구현 요약
### 1. FrameData 생성 (CameraX Analyzer)
CameraX의 ImageProxy 를 원본 이미지 크기 그대로 YUV(NV21) 로 변환하여 ML Kit으로 전달한다.

**⚠ FrameData.width/height 는 반드시 센서 원본 크기(mediaImage.width/height) 를 사용해야 함.**
```kotlin
val frame = FrameData(
    timestampMs = now,
    width = mediaImage.width,
    height = mediaImage.height,
    rotationDegrees = rotation,
    yuv = imageProxy.toYuvByteArray()
)
```

### 2. ML Kit 얼굴 인식 (MlKitFaceDetector)
```kotlin
val inputImage = InputImage.fromByteArray(
    frame.yuv,
    frame.width,
    frame.height,
    frame.rotationDegrees,      // 회전 정보 정확하게 전달
    InputImage.IMAGE_FORMAT_NV21
)

detector.process(inputImage)
```
ML Kit이 반환한 `boundingBox` 를 `FaceRect` 로 변환 후
`DetectedFaceInfo` 로 구성하여 반환한다.

### 3. 상태 관리(PowerMeasurementStateMachine)
#### 상태 정의
`PowerMeasurementState`는 이제 `Map<Int, FaceMeasurementState>`를 보유하며, 각 얼굴의 상태를 관리한다.

- `FaceMeasurementState.Idle`
- `FaceMeasurementState.Measuring(faceId, boundingBox, startTime, values)`
- `FaceMeasurementState.Done(faceId, boundingBox, averagedPower)`

#### 전환 규칙
- 각 얼굴(trackingId)별로 독립적인 상태 머신 동작
- 첫 얼굴 감지 → Measuring 시작
- 동일 trackingId 유지 → 전투력 누적
- 3초 경과 → Done
- 얼굴 사라짐 → 상태 맵에서 제거 (측정 중단)

### 4. 얼굴 오버레이 (Jetpack Compose Canvas)
#### FILL_CENTER 스케일링 구조 해결
PreviewView(FILL_CENTER)는 이미지가 중앙 기준으로 확대되고 일부가 잘릴 수 있기 때문에
ML Kit 좌표 → 화면 좌표 변환 시 scale + dx/dy 보정이 필요하다.
```kotlin
val scale = max(viewWidth / imageWidth, viewHeight / imageHeight)
val dx = (viewWidth - imageWidth * scale) / 2f
val dy = (viewHeight - imageHeight * scale) / 2f

val left = box.left * scale + dx
```

#### Measuring 상태 (회전 사각형 애니메이션)
```kotlin
rotate(rotation, pivot = Offset(cx, cy)) {
    drawRect(
        color = Color.Cyan,
        topLeft = Offset(cx - size/2, cy - size/2),
        size = Size(size, size),
        style = Stroke(width = 5.dp.toPx())
    )
}
```

#### Done 상태 (박스 + 전투력 텍스트)
```kotlin
drawRect(
    color = Color(0xFF00FF00),
    topLeft = Offset(left, top),
    size = Size(w, h),
    style = Stroke(5.dp.toPx())
)

drawText("전투력 ${power}", left, top - 20)
```

## 🧪 동작 흐름 요약
```
CameraX → ImageProxy → FrameData → ML Kit 분석
→ DetectedFaceInfo → StateMachine.update()
→ PowerMeasurementState → Compose Overlay 렌더링
```

## 🚨 문제 해결 과정 & 주요 버그 해결 내역
### ✔ 얼굴 없이도 오버레이가 표시됨
- Measuring 상태에서 얼굴이 사라져도 상태가 Idle로 돌아가지 않음
- mainFace == null 처리 시 Measuring → Idle 로 수정

### ✔ 얼굴 위치와 오버레이 위치가 어긋남
- FrameData.width/height 값을 rotation 기준으로 뒤집어서 ML Kit에 넘김
- ML Kit은 YUV stride 기준이므로 width/height 뒤집으면 boundingBox 깨짐
- rotationDegrees 를 ML Kit에 그대로 전달하는 방식으로 수정

### ✔ FILL_CENTER 스케일링 문제
- 단순 scaleX/scaleY 로 변환 시 위치가 어긋남
- scale + dx/dy 보정 공식 적용하여 정확히 매핑

### ✔ 전투력 측정이 끝나면 상태가 다시 Measuring 로 돌아옴
- Done 상태는 얼굴이 사라져도 유지하도록 로직 보완

## 향후 개선 계획
- 작성 예정

## 📸 결과 화면 및 공유
- 측정 완료된 얼굴 터치 시 결과 화면으로 이동
- 크롭된 얼굴 이미지와 전투력 표시
- 다시하기 및 공유하기 기능 제공

## 🛠 리팩토링
- `MainActivity`의 네비게이션 코드를 `CScouterNavGraph`로 분리하여 가독성 및 유지보수성 향상
- Coil 라이브러리 의존성을 제거하고 기본 `Image` 컴포저블과 `BitmapFactory`를 사용하여 이미지 로딩 구현
- 얼굴 인식 유예 시간(Grace Period)을 적용하여 터치 인식률 개선
- 결과 화면 이동 시 중복 클릭 방지(Debounce) 적용
- 터치 가능 영역 시각화 (반투명 초록색 채우기)
- 터치 편의성을 위해 터치 인식 영역을 얼굴 박스의 1.5배로 확장
- 결과 화면 공유 시 텍스트 대신 화면 전체 스크린샷 이미지 공유 기능 구현