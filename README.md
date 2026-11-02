# Лаборатори 8: Mockito mock объектууд

**Оюутан:** М.Мөнхгэрэлт  
**Оюутны код:** B232270012  
**Хичээл:** F.CSA313 - Программ хангамжийн чанарын баталгаа ба тест  

`java -version` командын гаралт:

```text
openjdk version "21.0.1" 2023-10-17 LTS
OpenJDK Runtime Environment Temurin-21.0.1+12 (build 21.0.1+12-LTS)
OpenJDK 64-Bit Server VM Temurin-21.0.1+12 (build 21.0.1+12-LTS, mixed mode, sharing)
```

`mvn -version` командын гаралт:

```text
Apache Maven 3.9.9 (8e8579a9e76f7d015ee5ec7bfcdc97d260186937)
Maven home: C:\Program Files\apache-maven-3.9.9
Java version: 21.0.1, vendor: Eclipse Adoptium, runtime: C:\Program Files\Eclipse Adoptium\jdk-21.0.1.12-hotspot
Default locale: en_US, platform encoding: UTF-8
OS name: "windows 11", version: "10.0", arch: "amd64", family: "windows"
```

## Зорилго ба орчин

`CourseRegistrationService.register()` методын бүртгэлийн логикийг өгөгдлийн сан, и-мэйл илгээх үйлчилгээнээс тусгаарлан шалгав. `StudentRepository`, `EmailSender` хоёр интерфейсийг Mockito mock объектоор орлуулж, шалгах нөхцөл бүрд шаардлагатай хариуг тохируулсан. Үйлчилгээ болон `Student` өгөгдлийг бодит объектоор ашигласан.

Тестийг Windows 11 дээр JDK 21, Maven 3.9.9 ашиглан ажиллуулсан. Төсөл Java 17-д зориулж хөрвүүлэгдэнэ; JUnit Jupiter 5.10.2, Mockito 5.11.0, Maven Surefire 3.2.5 хувилбарууд нь багшийн `pom.xml` файлд заасан хэвээр байна. `pom.xml` болон `src/main` доторх дөрвөн файлыг эцсийн хувилбарт өөрчлөөгүй.

## Ажиллуулах

JDK 17 ба түүнээс дээш хувилбар, Maven шаардлагатай. Эхний ажиллуулалтаар Maven хамаарлуудаа интернэтээс татна. Төслийн `pom.xml` байгаа хавтаснаас:

```sh
mvn test
```

Windows дээр хэд хэдэн JDK суусан бол PowerShell-ийн тухайн цонхонд ашиглах хувилбарыг дараах байдлаар сонгож болно. Доорх зам нь энэ туршилтад ашигласан суулгалтын зам юм.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.1.12-hotspot'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
mvn test
```

Гаралтыг PowerShell 7 дээр UTF-8 файлаар хадгалах команд:

```powershell
New-Item -ItemType Directory -Path results -Force | Out-Null
mvn -B -ntp '-Dstyle.color=never' test 2>&1 | Tee-Object -FilePath results/mvn-test.txt
```

JDK 21 дээр `byte-buddy-agent` динамикаар ачаалагдсан тухай анхааруулга гарсан. Энэ нь Mockito-ийн mock үүсгэх механизмтай холбоотой бөгөөд доорх ажиллуулалтуудад тестийн алдаа болоогүй.

## Хэсэг A: Нэгжийн тестүүд

Тестийн класс: [CourseRegistrationServiceTest.java](src/test/java/mn/edu/must/sqat/CourseRegistrationServiceTest.java). `MockitoExtension` нь mock объектуудыг бэлтгэж, `@BeforeEach` нь тест бүрд үйлчилгээг шинээр үүсгэнэ.

| Тест | Шалгасан нөхцөл ба хүлээлт | Ашигласан гол хэрэгсэл |
|---|---|---|
| A1 | Идэвхтэй, өмнөх холбоо хичээлээ үзсэн оюутанд `OK` буцна; бүртгэлийг нэг удаа хадгалж, зөв хаяг, гарчиг, агуулгатай и-мэйл илгээнэ | `when`, `thenReturn`, `verify` |
| A2 | Оюутан олдохгүй бол `ERROR_NO_STUDENT`; бүртгэл хадгалахгүй, и-мэйл илгээхгүй | `verify(..., never())` |
| A3 | Идэвхгүй оюутанд `ERROR_INACTIVE_STUDENT`; хадгалалт болон и-мэйл илгээлт хийгдэхгүй | `never`, `verifyNoInteractions` |
| A4 | Хоёр өмнөх холбоо хичээлийн нэг нь дутуу бол `ERROR_PREREQUISITES`; хадгалахгүй, и-мэйлтэй харьцахгүй | `when`, `never`, `verifyNoInteractions` |
| A5 | И-мэйл илгээгч `RuntimeException` шидсэн ч `OK` буцаж, бүртгэл хадгалах дуудлага хийгдсэн байна | `doThrow`, `verify` |
| A6 | Зөвхөн `studentId`, зөвхөн `courseId`, хоёулаа `null` байх гурван тохиолдолд `IllegalArgumentException`; хамаарлуудтай харьцахгүй | `assertAll`, `assertThrows`, `verifyNoInteractions` |
| A7 | Илгээсэн и-мэйлийн биеийг барьж, бүртгүүлсэн хичээлийн код орсон эсэхийг шалгана | `ArgumentCaptor`, `assertTrue` |
| A8 | Оюутан олдохгүй, оюутан идэвхгүй, хичээл олдохгүй гэсэн гурван ангид тохирох алдааг буцааж, хадгалалт болон и-мэйл илгээлт хийхгүй | `@ParameterizedTest`, `@CsvSource` |

A8-ийн `courseExists()` stub-ийг `lenient()` болгосон, учир нь эхний хоёр мөр оюутны шалгалтаас шууд буцаж, тухайн stub-ийг ашигладаггүй. Ингэснээр эдгээр мөрд `UnnecessaryStubbingException` үүсэхгүй бөгөөд бусад stub-ийн хатуу шалгалт хэвээр үлдэнэ.

Нэг дуудалтад matcher ашигласан үед бүх аргументад matcher хэрэглэсэн. Жишээлбэл, A7-д хаяг ба гарчгийг `eq(...)`, и-мэйлийн биеийг `body.capture()` ашиглан шалгасан.

Нийт **10 тестийн гүйцэтгэл** байна: A1-A7-ийн долоон энгийн тест, A8-ийн гурван мөр. Кодыг сэргээсний дараах [эцсийн Maven гаралт](results/mvn-test.txt):

```text
Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## Хэсэг B: Мини мутаци

Мутаци бүрийг анхны код дээр тус тусад нь оруулж, `mvn test` ажиллуулан гаралтыг хадгалсан. Дараагийн мутацийг эхлүүлэхийн өмнө эх файлыг сэргээсэн. Тестийн тоо гурван ажиллуулалтад бүгд 10 байсан.

| Мутант | Түр оруулсан өөрчлөлт | Унасан тестүүд, Maven гаралт дахь нэр | Failures / Errors | Нотолгоо |
|---|---|---|---|---|
| 1 | Идэвхгүй оюутны шалгалтыг хассан | A3: `register_inactiveStudent_returnsInactiveWithoutSavingOrEmail`; A8-ийн 2-р мөр: `register_invalidStudentOrCourse_returnsExpectedErrorWithoutSideEffects(...)[2]` | 2 / 0 | [mvn-test-mutant-1.txt](results/mvn-test-mutant-1.txt) |
| 2 | `saveRegistration(...)` дуудалтыг хассан | A1: `register_activeStudentWithPrerequisites_returnsOkAndNotifies`; A5: `register_emailSenderThrows_returnsOkAndKeepsRegistration` | 2 / 0 | [mvn-test-mutant-2.txt](results/mvn-test-mutant-2.txt) |
| 3 | Оролтын шалгалтаас `|| courseId == null` хэсгийг хассан | A6: `register_eitherArgumentNull_throwsWithoutDependencyInteractions` | 1 / 0 | [mvn-test-mutant-3.txt](results/mvn-test-mutant-3.txt) |

Гурван файлд бүгд `BUILD FAILURE` гарсан. Эхний мутантын үед A3 нь `ERROR_INACTIVE_STUDENT` хүлээсэн ч `ERROR_NO_COURSE`, A8-ийн хоёрдугаар мөр нь `OK` авсан тул унав. Хоёрдугаар мутантын үед буцаасан `OK` утга хэвээр байсан ч A1, A5-ийн `verify(repository).saveRegistration(...)` шалгалтууд хадгалалт алга болсныг илрүүлсэн.

Гуравдугаар мутантыг A6-ийн `register(STUDENT_ID, null)` шалгалт барьсан: `IllegalArgumentException` хүлээсэн боловч exception шидэгдээгүй. A6-д анхнаас нь аргумент тус бүрийн `null` болон хоёулаа `null` байх нөхцөлийг оруулсан учраас мутацийн дараа тестийг засах шаардлага гараагүй. Эдгээр уналт нь assertion болон `verify` шалгалтаас үүссэн; `UnnecessaryStubbingException` гараагүй.

Эцэст нь `src/main` доторх дөрвөн файлыг `starter.zip` дэх эхтэй байт бүрээр нь харьцуулж, ижил байгааг шалгасан. Сэргээсэн код дээр дахин ажиллуулахад 10 тест бүгд амжилттай болсон нь [mvn-test.txt](results/mvn-test.txt) файлд хадгалагдсан.

## Файлын бүтэц

```text
Lab8/
├── .gitignore
├── README.md
├── pom.xml
├── results/
│   ├── mvn-test.txt
│   ├── mvn-test-mutant-1.txt
│   ├── mvn-test-mutant-2.txt
│   └── mvn-test-mutant-3.txt
└── src/
    ├── main/java/mn/edu/must/sqat/
    │   ├── CourseRegistrationService.java
    │   ├── EmailSender.java
    │   ├── Student.java
    │   └── StudentRepository.java
    └── test/java/mn/edu/must/sqat/
        └── CourseRegistrationServiceTest.java
```

`target/`, `.idea/`, `.DS_Store`, `*.iml` файлуудыг `.gitignore`-д оруулсан. Тест, тайланг AI туслахын дэмжлэгтэй бэлтгэж, Maven-аар ажиллуулсан бодит гаралтуудыг `results/` хавтаст хадгалсан.

## Дүгнэлт

Mockito ашигласнаар өгөгдлийн сан болон SMTP сервергүйгээр бүртгэлийн үйлчилгээний үндсэн нөхцөлүүдийг тусгаарлан шалгаж чадсан. Mock объектын хариу болон дуудлагыг тест дотроос тохируулж шалгадаг бол гараар бичсэн fake нь интерфейсийн хялбаршуулсан, ажилладаг хэрэгжүүлэлт байдаг. `Student` зэрэг энгийн өгөгдлийн объектыг бодитоор үүсгэх боломжтой учраас mock болгох шаардлагагүй. Мөн тестийн бай болсон `CourseRegistrationService`-ийг mock болгочихвол бодит логикийг нь шалгаж чадахгүй. `verify`-г бүх дотоод дуудлагад хэт хэрэглэвэл үр дүн нь өөрчлөгдөөгүй жижиг засварын дараа ч тестүүд унах эрсдэлтэй. Тиймээс энэ ажилд хадгалалт, мэдэгдэл илгээх болон хориглосон үед эдгээр үйлдэл хийгдээгүй байх зэрэг шаардлагад хамаарах харилцааг шалгасан. Гурван мутаци бүгд баригдсан нь сонгосон шалгалтууд ажиллаж байгааг харуулсан боловч бүх боломжит алдааг илрүүлнэ гэсэн баталгаа болохгүй.
