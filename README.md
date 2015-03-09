# Modelchecker

Prototyp eines Modelcheckers

## Ziele

Der Modelchecker soll echte™ verteilte Anwendungen überprüfen können. Echt™ wird hier zur Abgrenzung von Modellen verwendet.

Echte™ Software zeichnet unter anderem folgendes aus:

1. Sie ist lauffähig
2. kann vorhandene Programmbibliotheken einbinden
3. verwaltet komplexe Datenstrukturen zusammen gesetzten aus: Integer-Derivaten (long, char, bool ...), Algebraischen Datentypen
4. es gibt mindestens eine etablierte Lösungen für asynchrone Datenverarbeitung, von der ausgegangen werden kann, dass sie sich spezifikationsgemäß verhält. Beispiele wäre unter anderen: Transaktionen, Pipes, Aktoren, Monitore, Futures
5. Sie hat eine modularen Aufbau.
6. Es kann eine Problemdomäne geben, die außerhalb der Informatik liegt.

## Beispiel

Ein Anwender will ein Zusatzfeature für seine Anwendung buchen. Um die Bezahlung zu organisieren, wird ein bereits exsistierender Bezahldienst verwendet.

Da all Instanzen (Anwender, Anwendung, Bezahldienst) nur per Netzwerk verbunden sind, muss davon ausgegangen werden, dass einzelne Anfragen verloren gehen.

Nun könnten folgende (nicht testbaren) Anforderungen könnten von Interesse sein:
* Wenn keine Anfragen verloren gehen (d.h. auch, der Bezahldienst ist nicht offline), dann gibt es für den Kunden keine Möglichkeit die Zusatzfeatures zu verwenden ohne zuvor bezahlt zu haben.
* Selbst wenn Bezahldienst zu einen beliebigen Zeitpunkt nicht mehr erreichbar ist, kann es nicht dazu kommen, dass der Anwender Geld verloren hat ohne das Zusatzfeature bekommen zu haben. Das Gegenteil ist aber erlaubt. (Er hat das Feature bekommen, musste aber dafür nicht bezahlen)
* Zwei Benutzer können sich nicht gegenseitig beeinflussen.
