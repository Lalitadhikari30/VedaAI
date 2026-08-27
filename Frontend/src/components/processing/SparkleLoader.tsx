export default function SparkleLoader() {
  return (
    <div className="flex flex-col items-center justify-center py-12 px-6 select-none">
      {/* Animated Starburst Cluster */}
      <div className="relative w-36 h-36 md:w-44 md:h-44 mb-6 flex items-center justify-center">
        {/* Small Dot */}
        <div className="sparkle-dot absolute top-6 left-6 md:top-8 md:left-8">
          <div className="w-3.5 h-3.5 rounded-full bg-[#E8623C] shadow-xs" />
        </div>

        {/* Medium 4-point Star (Bottom Left) */}
        <div className="sparkle-small absolute bottom-6 left-8 md:bottom-8 md:left-10">
          <svg width="46" height="46" viewBox="0 0 50 50" className="drop-shadow-xs">
            <path
              d="M25 0 C27 16 34 23 50 25 C34 27 27 34 25 50 C23 34 16 27 0 25 C16 23 23 16 25 0 Z"
              fill="#E8623C"
            />
          </svg>
        </div>

        {/* Main Large 4-point Star (Top Right / Center) */}
        <div className="sparkle-main absolute top-2 right-4 md:top-4 md:right-6">
          <svg width="74" height="74" viewBox="0 0 80 80" className="drop-shadow-sm">
            <path
              d="M40 0 C43 26 54 37 80 40 C54 43 43 54 40 80 C37 54 26 43 0 40 C26 37 37 26 40 0 Z"
              fill="#E8623C"
            />
          </svg>
        </div>

        {/* Tiny sparkle accent */}
        <div className="sparkle-dot absolute bottom-10 right-8">
          <div className="w-2.5 h-2.5 rounded-full bg-[#E8623C]/80" />
        </div>
      </div>

      {/* Bold Extracting Text */}
      <h2 className="text-2xl md:text-3xl font-black text-[#1C1C1C] tracking-tight mb-2">
        Extracting...
      </h2>
      <p className="text-[#6B6B68] text-sm md:text-base font-medium">
        This may take a while
      </p>
    </div>
  );
}
