$(document).ready(function () {
    console.log("installing section h1");
    $("h1[id^=section]").each(function(){
        console.log("         "+$(this));
    });
    $("h1[id^=section]").click(function () {
        console.log("    toggle "+$(this).attr('id')+" "+$("div#"+$(this).attr('id')));
        $("div#"+$(this).attr('id')).toggle(0, function () {
        })
    })
});