
# This; box; will; attempt; to; connect; to; the; most; recently; opened; ipython; notebook; session (when; this; box; is; loaded)

# -------------------------;
# We; can't right now sign messages sent to the kernel the way that ipython wants, so you need to add this line to your ipython_notebook_config.py file (found, typically, ~/.ipython/profile_default/ipython_notebook_config.py);;
# c.Session.key = b'';;

# then; you; can; do things; like; this
%pylab; inline;
x = np.linspace(0, 10, 1000);

fig, ax = plt.subplots();
lines = ax.plot(x, np.sin(x));
print; lines;

# completion; and; inspection (ctrl-.) work; just; fine as well;







