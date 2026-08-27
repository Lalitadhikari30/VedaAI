export default function SparkleLoader() {
  return (
    <div className="flex flex-col items-center justify-center select-none">
      {/* Animated Starburst Cluster matching Image 2 exactly */}
      <div className="relative w-36 h-36 md:w-44 md:h-44 mb-4 flex items-center justify-center">
        {/* Small Coral Dot (Top Left) */}
        <div className="sparkle-dot absolute top-8 left-6 md:top-10 md:left-8">
          <div className="w-3.5 h-3.5 rounded-full bg-[#E8623C]" />
        </div>

        {/* Medium 4-point Star (Bottom Left) */}
        <div className="sparkle-small absolute bottom-5 left-7 md:bottom-7 md:left-9">
          <svg width="44" height="44" viewBox="0 0 50 50">
            <path
              d="M25 0 C27 16 34 23 50 25 C34 27 27 34 25 50 C23 34 16 27 0 25 C16 23 23 16 25 0 Z"
              fill="#E8623C"
            />
          </svg>
        </div>

        {/* Main Large 4-point Star (Top Right / Center) */}
        <div className="sparkle-main absolute top-2 right-4 md:top-3 md:right-6">
          <svg width="72" height="72" viewBox="0 0 80 80">
            <path
              d="M40 0 C43 26 54 37 80 40 C54 43 43 54 40 80 C37 54 26 43 0 40 C26 37 37 26 40 0 Z"
              fill="#E8623C"
            />
          </svg>
        </div>

        {/* Tiny 4-point Star Accent (Bottom Right) */}
        <div className="sparkle-dot absolute bottom-11 right-6 md:bottom-12 md:right-8">
          <svg width="18" height="18" viewBox="0 0 30 30">
            <path
              d="M15 0 C16 10 20 14 30 15 C20 16 16 20 15 30 C14 20 10 16 0 15 C10 14 14 10 15 0 Z"
              fill="#F28263"
            />
          </svg>
        </div>
      </div>

      {/* Bold Extracting Text */}
      <h2 className="text-[22px] md:text-[25px] font-black text-[#1C1C1C] tracking-tight mb-1">
        Extracting...
      </h2>
      <p className="text-[#8A8A87] text-[14px] md:text-[15px] font-normal">
        This may take a while
      </p>
    </div>
  );
}
