// %%NO_OVERLOADS%%
var HandlesForFLines = Java.type('fieldbox.boxes.plugins.HandlesForFLines');
var DraggableNode = Java.type('fieldbox.boxes.plugins.HandlesForFLines.DraggableNode');


_.once._simpleTweaks = () => "//%%NO_OVERLOADS%%\n";

_.exec(_._simpleTweaks);

for (var e of _.lines.entrySet()) {
    let name = e.getKey();
    let val = e.getValue();
    if (val.tweaky)
        for (var n of e.getValue().nodes)
            d = new DraggableNode(_, val, n,
                s => {
                    desc = s.describe("HandlesForFLines", "_.lines['" + name+"']");
                    _._simpleTweaks += desc + "\n";
                    _.next.update = () => {
                        _()
                    }
                })
}
