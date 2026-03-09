#!/bin/bash

# Default model
MODEL="kilo/z-ai/glm-5:free"
STATE_FILE="processed_units.txt"

# Použijeme zadaný model, pokud je poskytnut jako argument
if [ ! -z "$1" ]; then
    MODEL="$1"
fi

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
UNITS_DIR="$SCRIPT_DIR/units"
STATE_FILE_PATH="$SCRIPT_DIR/$STATE_FILE"

# Zaručujeme, že soubor existuje, aby šel číst grepem bez chyby
touch "$STATE_FILE_PATH"

echo "Using model: $MODEL"
echo "State file: $STATE_FILE_PATH"

# Získat seznam složek začínajících 'unit-' a seřadit je
for UNIT_DIR in $(ls -d "$UNITS_DIR"/unit-* | sort); do
    UNIT_NAME=$(basename "$UNIT_DIR")
    
    # Zkontrolujeme, zda se název unity nachází v txt souboru
    if grep -Fxq "$UNIT_NAME" "$STATE_FILE_PATH"; then
        echo -e "\033[1;30mPřeskakuji $UNIT_NAME (již zpracováno).\033[0m"
        continue
    fi
    
    echo -e "\033[0;36m========================================\033[0m"
    echo -e "\033[0;32mProcessing $UNIT_NAME...\033[0m"
    echo -e "\033[0;36m========================================\033[0m"
    
    PROMPT="Aktuální testy v units/$UNIT_NAME mají kriticky nízkou coverage (často pod 2%). Cílem je dosáhnout test coverage >90%. Vytvoř nebo doplň chybějící unit testy pro $UNIT_NAME (s využitím xUnit a Moq), které pokryjí maximální možné množství logiky. Vygeneruj skutečný funkční kód testů a ujisti se, že projdou."
    
    echo -e "\033[1;33mExecuting kilocode for $UNIT_NAME\033[0m"
    kilocode run -m "$MODEL" "$PROMPT"
    
    EXIT_CODE=$?
    if [ $EXIT_CODE -ne 0 ]; then
        echo -e "\033[0;31mWarning: kilocode command returned non-zero exit code ($EXIT_CODE) for $UNIT_NAME.\033[0m"
    fi
    
    # Zapíše dokončenou unitu na poslední řádek souboru
    echo "$UNIT_NAME" >> "$STATE_FILE_PATH"
done

echo -e "\033[0;32mDokončeno.\033[0m"