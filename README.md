# LandMC Cosmetics

Dodatki, które gracz kupuje za diamenty i nosi na sobie: cząsteczki i świecenie.

## Podział, który nie jest oczywisty

To repozytorium **nie zawiera sklepu**. Zawiera tylko to, co rysuje.

Dodatki kupuje się za diamenty, a diamenty są w
[`landmc-economy`](https://github.com/landmc-network/landmc-economy) — tam już stoi sklep rang
i rangi wizualne, i tam jest ten jeden ostrożny kawałek SQL-a, który zdejmuje komuś pieniądze.
Drugi plugin obciążający tę samą tabelę byłby drugą opinią o czyimś saldzie, więc katalog,
ceny, własność i wybór zostają po tamtej stronie.

Tutaj jest to, czego proxy zrobić nie może: narysować. Cząsteczki i świecenie to Bukkit, a proxy
nie ma jak ich dotknąć.

| moduł | co to jest |
|---|---|
| `cosmetics-api` | wiadomość między sklepem a backendem, publikowana jako artefakt |
| `cosmetics-paper` | plugin, który rysuje to, co mu powiedziano |

## Efekt jedzie w wiadomości

`CosmeticEffect` niesie **co dodatek robi**, a nie tylko jego identyfikator — cząsteczkę i wzór
albo kolor świecenia. Alternatywą byłby katalog po obu stronach: ceny i nazwy na proxy,
cząsteczki i kolory na backendzie, a wtedy dołożenie jednego dodatku to edycja dwóch plików,
które muszą zgadzać się co do identyfikatora, którego nikt nie sprawdza.

Konsekwencja: **katalog jest w jednym miejscu**, a backend, który nigdy nie słyszał o danym
dodatku, i tak go narysuje.

## Świecenie kontra scoreboard

Minecraft nie ma „świeć na czerwono". Ma flagę świecenia na encji i rysuje obrys w kolorze
drużyny, do której ta encja należy. Kolorowe świecenie to więc drużyna na kolor i ktoś do niej
wpisany.

I tu robi się niewygodnie: drużyny należą do scoreboardu, a lobby daje **każdemu graczowi
własny**, żeby plansza mogła się różnić. Drużyna musi więc istnieć na tablicy każdego, kto może
patrzeć — nie raz na wspólnej. Dlatego ta praca jest per widz i musi się powtórzyć, kiedy komuś
podmieni się tablicę.

Drużyny mają własny przedrostek i trzymają wyłącznie świecących graczy, więc nic innego na tych
tablicach nie jest ruszane.

## Wydajność

Cząsteczki rysuje **jedno zadanie na serwer**, chodzące po graczach, którzy coś noszą — nie
zadanie na gracza. Wzór jest funkcją czasu, nie zapamiętanej pozycji, więc między klatkami nic
nie trzeba pamiętać i nic nie zostaje po kimś, kto się rozłączył.

Każda cząsteczka idzie do graczy, którzy mogą ją zobaczyć, a nie do świata: wrzucenie jej do
świata wysyła ją wszystkim w zasięgu renderowania, niezależnie od tego, czy efekt jest dla nich.
