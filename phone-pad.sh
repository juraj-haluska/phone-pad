while IFS= read -r line; do    
    IFS=':' read -r -a vals <<< "$line"
    xdotool mousemove_relative -- ${vals[0]} ${vals[1]}
done < /dev/tcp/192.168.10.100/8000